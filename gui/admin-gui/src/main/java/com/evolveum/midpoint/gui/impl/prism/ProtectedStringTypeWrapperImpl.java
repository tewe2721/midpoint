/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import java.util.Collection;

/**
 * Created by honchar
 */
public class ProtectedStringTypeWrapperImpl extends PrismPropertyWrapperImpl<ProtectedStringType>{
    private static final long serialVersionUID = 1L;

    private static final transient Trace LOGGER = TraceManager.getTrace(ProtectedStringTypeWrapperImpl.class);

    public ProtectedStringTypeWrapperImpl(PrismContainerValueWrapper<?> parent, PrismProperty<ProtectedStringType> item, ItemStatus status) {
        super(parent, item, status);

//        getItem().setRealValue(null);
    }

    @Override
    public <D extends ItemDelta<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>>> Collection<D> getDelta() throws SchemaException {
        PrismPropertyValueWrapper<ProtectedStringType> valueWrapper = getValue();
        if (valueWrapper != null && valueWrapper.getRealValue() == null && valueWrapper.getOldValue().getRealValue() != null){
            valueWrapper.setStatus(ValueStatus.DELETED);
        }
        return super.getDelta();
    }


}
