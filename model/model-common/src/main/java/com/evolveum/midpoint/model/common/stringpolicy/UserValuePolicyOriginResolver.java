/**
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.stringpolicy;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class UserValuePolicyOriginResolver extends AbstractValuePolicyOriginResolver<UserType> {

    public UserValuePolicyOriginResolver(PrismObject<UserType> object, ObjectResolver objectResolver) {
        super(object, objectResolver);
    }

    @Override
    public ObjectQuery getOwnerQuery() {
        return getObject().getPrismContext()
                .queryFor(UserType.class)
                .item(UserType.F_PERSONA_REF).ref(getObject().getOid())
                .build();
    }
}
