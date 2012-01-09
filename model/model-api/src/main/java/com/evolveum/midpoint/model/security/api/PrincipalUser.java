/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.security.api;

import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * Temporary place, till we create special component for it
 *
 * @author lazyman
 * @author Igor Farinic
 */
public class PrincipalUser implements Serializable {

    private static final long serialVersionUID = 8299738301872077768L;
    private UserType user;
    private Credentials credentials;
    private boolean enabled;

    public PrincipalUser(UserType user, boolean enabled) {
        Validate.notNull(user, "User must not be null.");
        this.user = user;
        this.enabled = enabled;
    }

    public UserType getUser() {
        return user;
    }

    public String getName() {
        return getUser().getName();
    }

    public String getFamilyName() {
        return getUser().getFamilyName();
    }

    public String getFullName() {
        return getUser().getFullName();
    }

    public String getGivenName() {
        return getUser().getGivenName();
    }

    public Credentials getCredentials() {
        if (credentials == null) {
            credentials = new Credentials();
        }

        return credentials;
    }

    void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOid() {
        return getUser().getOid();
    }
}
