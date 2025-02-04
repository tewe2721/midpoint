/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * @author semancik
 *
 */
@Component
public class ScriptExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    public static final QName ELEMENT_NAME = new ObjectFactory().createScript(new ScriptExpressionEvaluatorType()).getName();

    @Autowired private ScriptExpressionFactory scriptExpressionFactory;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private LocalizationService localizationService;
    @Autowired private Protector protector;
    @Autowired private PrismContext prismContext;

    public ScriptExpressionEvaluatorFactory() {
        super();
        // Nothing here
    }

    // For use in tests
    public ScriptExpressionEvaluatorFactory(ScriptExpressionFactory scriptExpressionFactory,
            SecurityContextManager securityContextManager, PrismContext prismContext) {
        this.scriptExpressionFactory = scriptExpressionFactory;
        this.securityContextManager = securityContextManager;
        this.prismContext = prismContext;
    }

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.ItemDefinition)
     */
    @Override
    public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(Collection<JAXBElement<?>> evaluatorElements,
            D outputDefinition, ExpressionProfile expressionProfile, ExpressionFactory factory, String contextDescription, Task task, OperationResult result) throws SchemaException, SecurityViolationException {

        if (evaluatorElements.size() > 1) {
            throw new SchemaException("More than one evaluator specified in "+contextDescription);
        }
        JAXBElement<?> evaluatorElement = evaluatorElements.iterator().next();

        Object evaluatorElementObject = evaluatorElement.getValue();
        if (!(evaluatorElementObject instanceof ScriptExpressionEvaluatorType)) {
            throw new IllegalArgumentException("Script expression cannot handle elements of type " + evaluatorElementObject.getClass().getName());
        }
        ScriptExpressionEvaluatorType scriptType = (ScriptExpressionEvaluatorType) evaluatorElementObject;

        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(scriptType, outputDefinition, expressionProfile, factory, contextDescription, task, result);

        return new ScriptExpressionEvaluator<V,D>(ELEMENT_NAME, scriptType, outputDefinition, protector, prismContext, scriptExpression, securityContextManager, localizationService);

    }

}
