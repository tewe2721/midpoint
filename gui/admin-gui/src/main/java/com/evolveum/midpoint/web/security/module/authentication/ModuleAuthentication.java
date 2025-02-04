/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.authentication;

import com.evolveum.midpoint.web.security.util.ModuleType;
import com.evolveum.midpoint.web.security.util.StateOfModule;
import org.springframework.security.core.Authentication;

/**
 * @author skublik
 */

public class ModuleAuthentication {

    private Authentication authentication;

    private String nameOfModule;

    private ModuleType type;

    private StateOfModule state;

    private String prefix;

    public ModuleAuthentication() {
        setState(StateOfModule.LOGIN_PROCESSING);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getNameOfModule() {
        return nameOfModule;
    }

    public void setNameOfModule(String nameOfModule) {
        this.nameOfModule = nameOfModule;
    }

    public ModuleType getType() {
        return type;
    }

    protected void setType(ModuleType type) {
        this.type = type;
    }

    public StateOfModule getState() {
        return state;
    }

    public void setState(StateOfModule state) {
        this.state = state;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public ModuleAuthentication clone() {
        ModuleAuthentication module = new ModuleAuthentication();
        clone(module);
        return module;
    }

    protected void clone (ModuleAuthentication module) {
        module.setState(this.getState());
        module.setAuthentication(this.getAuthentication());
        module.setNameOfModule(this.nameOfModule);
        module.setType(this.getType());
        module.setPrefix(this.getPrefix());
    }

}
