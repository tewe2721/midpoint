/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.authentication.MidPointUserProfilePrincipal;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.model.api.context.PreAuthenticationContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.MidpointAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml.saml2.attribute.Attribute;
import org.springframework.security.saml.spi.DefaultSamlAuthentication;

import java.util.List;

/**
 * @author skublik
 */

public class MidpointSaml2Provider extends MidPointAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointSaml2Provider.class);

    @Autowired
    private transient AuthenticationEvaluator<PasswordAuthenticationContext> authenticationEvaluator;

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List<ObjectReferenceType> requireAssignment) throws AuthenticationException {
        ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_USER_URI);

        try {
            Authentication token;
            if (authentication instanceof DefaultSamlAuthentication) {
                DefaultSamlAuthentication samlAuthentication = (DefaultSamlAuthentication) authentication;
                Saml2ModuleAuthentication samlModule = (Saml2ModuleAuthentication) SecurityUtils.getProcessingModule(true);
                List<Attribute> attributes = ((DefaultSamlAuthentication) authentication).getAssertion().getAttributes();
                String enteredUsername = null;
                for (Attribute attribute : attributes) {
                    if (attribute.getFriendlyName().equals(samlModule.getNamesOfUsernameAttributes().get(samlAuthentication.getAssertingEntityId()))) {
                        List<Object> values = attribute.getValues();
                        if (values == null) {
                            LOGGER.error("Saml attribute, which define username don't contains value");
                            throw new AuthenticationServiceException("web.security.auth.saml2.username.null");
                        }
                        if (values.size() != 1) {
                            LOGGER.error("Saml attribute, which define username contains more values {}", values);
                            throw new AuthenticationServiceException("web.security.auth.saml2.username.more.values");
                        }
                        enteredUsername = (String) values.iterator().next();
                    }
                }
                Validate.notBlank(enteredUsername);
                token = authenticationEvaluator.authenticateUserPreAuthenticated(connEnv, new PreAuthenticationContext(enteredUsername, requireAssignment));
            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }

            MidPointPrincipal principal = (MidPointPrincipal)token.getPrincipal();

            LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                    authentication.getClass().getSimpleName(), principal.getAuthorities());
            return token;

        } catch (AuthenticationException e) {
            LOGGER.info("Authentication with saml module failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    protected void writeAutentication(Authentication originalAuthentication, MidpointAuthentication mpAuthentication,
                                      ModuleAuthentication moduleAuthentication, Authentication token) {
        Object principal = token.getPrincipal();
        if (principal != null && principal instanceof MidPointUserProfilePrincipal) {
            mpAuthentication.setPrincipal((MidPointUserProfilePrincipal) principal);
        }

        moduleAuthentication.setAuthentication(originalAuthentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        if (DefaultSamlAuthentication.class.equals(authentication)) {
            return true;
        }

//        if (MidpointAuthentication.class.equals(authentication)) {
//            return true;
//        }

        return false;
    }
}
