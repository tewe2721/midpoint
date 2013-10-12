/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.component.data.column.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class DebugObjectItem extends Selectable implements InlineMenu {

    public static final String F_OID = "oid";
    public static final String F_NAME = "name";
    public static final String F_RESOURCE_NAME = "resourceName";
    public static final String F_RESOURCE_TYPE = "resourceType";
    public static final String F_FULL_NAME = "fullName";
    public static final String F_DESCRIPTION = "description";

    private String oid;
    private String name;
    private String description;

    //todo create subclasses
    private String resourceName;
    private String resourceType;

    private String fullName;

    public DebugObjectItem(String oid, String name, String description) {
        this.name = name;
        this.oid = oid;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getOid() {
        return oid;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }

    public static DebugObjectItem createDebugObjectItem(PrismObject object) {
        DebugObjectItem item = new DebugObjectItem(object.getOid(), WebMiscUtil.getName(object),
                (String) object.getPropertyRealValue(ObjectType.F_DESCRIPTION, String.class));

        if (UserType.class.isAssignableFrom(object.getCompileTimeClass())) {
            PolyString fullName = WebMiscUtil.getValue(object, UserType.F_FULL_NAME, PolyString.class);
            item.setFullName((fullName != null ? fullName.getOrig() : null));
        }

        return item;
    }

    @Override
    public List<InlineMenuItem> getMenuItems() {
        return new ArrayList<InlineMenuItem>();
    }
}
