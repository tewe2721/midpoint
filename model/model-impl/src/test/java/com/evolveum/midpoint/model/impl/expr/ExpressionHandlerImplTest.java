/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConditionalSearchFilterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import javax.xml.namespace.QName;

/**
 *
 * @author lazyman
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ExpressionHandlerImplTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionHandlerImplTest.class);
    private static final File TEST_FOLDER = new File("./src/test/resources/expr");
    private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");

    @Autowired
    private ExpressionHandler expressionHandler;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);

        // just something to fill into c:actor expression variable
        MidPointPrincipal principal = new MidPointPrincipal(new UserType(PrismTestUtil.getPrismContext()));
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);
        securityContext.setAuthentication(authentication);
    }

    // This test is wrong. Maybe wrong place.
    // But the problem is, that the account here contains raw values. It does not have
    // the definition applied. Therefore the equals() in groovy won't work.
    @Test(enabled=false)
    @SuppressWarnings("unchecked")
    public void testConfirmUser() throws Exception {
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(
                TEST_FOLDER, "account-xpath-evaluation.xml"));

        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_FOLDER, "user-new.xml"));

        //TODO:  "$c:user/c:givenName/t:orig replaced with "$c:user/c:givenName
        ExpressionType expression = PrismTestUtil.parseAtomicValue(
                "<object xsi:type=\"ExpressionType\" xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                        + "<script>\n"
                        + "<trace>true</trace>\n"
                        + "<code>"
                        + "basic.getAttributeValues(projection, \"givenName\")?.contains(user.getGivenName().getOrig())"
                        + "</code>"
                        + "</script>"
                        + "</object>", ExpressionType.COMPLEX_TYPE);

        OperationResult result = new OperationResult("testConfirmUser");
        boolean confirmed = expressionHandler.evaluateConfirmationExpression(user.asObjectable(), account.asObjectable(), expression,
                null, result);
        LOGGER.info(result.debugDump());

        assertTrue("Wrong expression result (expected true)", confirmed);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEvaluateExpression() throws Exception {
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(TEST_FOLDER, "account.xml"));
        ShadowType accountType = account.asObjectable();
        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(TEST_FOLDER_COMMON, "resource-dummy.xml"));
        ResourceType resourceType = resource.asObjectable();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.asReferenceValue().setObject(resource);
        accountType.setResourceRef(resourceRef);

        ObjectSynchronizationType synchronization = resourceType.getSynchronization().getObjectSynchronization().get(0);
        for (ConditionalSearchFilterType filter : synchronization.getCorrelation()){
            MapXNode clauseXNode = filter.getFilterClauseXNode();
            // key = q:equal, value = map (path + expression)
            RootXNode expressionNode = ((MapXNode) clauseXNode.getSingleSubEntry("filter value").getValue())
                    .getEntryAsRoot(new QName(SchemaConstants.NS_C, "expression"));

            ExpressionType expression = PrismTestUtil.getPrismContext().parserFor(expressionNode).parseRealValue(ExpressionType.class);
            LOGGER.debug("Expression: {}",SchemaDebugUtil.prettyPrint(expression));

            OperationResult result = new OperationResult("testCorrelationRule");
            String name = expressionHandler.evaluateExpression(accountType, expression, "test expression", null, result);
            LOGGER.info(result.debugDump());

            assertEquals("Wrong expression result", "hbarbossa", name);
        }
    }

    private Element findChildElement(Element element, String namespace, String name) {
        NodeList list = element.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && namespace.equals(node.getNamespaceURI())
                    && name.equals(node.getLocalName())) {
                return (Element) node;
            }
        }
        return null;
    }
}
