/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.authentication;

import com.evolveum.midpoint.web.security.util.ModuleType;
import com.evolveum.midpoint.web.security.util.StateOfModule;

/**
 * @author skublik
 */

public class LoginFormModuleAuthentication extends ModuleAuthentication {

    public LoginFormModuleAuthentication() {
        setType(ModuleType.LOCAL);
        setState(StateOfModule.LOGIN_PROCESSING);
    }

    public ModuleAuthentication clone() {
        LoginFormModuleAuthentication module = new LoginFormModuleAuthentication();
        clone(module);
        return module;
    }
}
