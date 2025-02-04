/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;

/**
 * @author katka
 *
 */
public class PrismReferencePanelContext<R extends Referencable> extends ItemPanelContext<R, PrismReferenceWrapper<R>>{

    public PrismReferencePanelContext(IModel<PrismReferenceWrapper<R>> itemWrapper) {
        super(itemWrapper);
    }

    public ObjectFilter getFilter() {
        return unwrapWrapperModel().getFilter();
    }

    public List<QName> getTargetTypes() {
        return unwrapWrapperModel().getTargetTypes();
    }

    public QName getTargetTypeName() {
        return unwrapWrapperModel().getTargetTypeName();
    }
}
