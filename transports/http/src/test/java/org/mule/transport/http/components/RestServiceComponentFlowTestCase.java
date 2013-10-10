/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;

import org.junit.Test;
import org.mule.api.component.Component;
import org.mule.api.expression.ExpressionManager;
import org.mule.construct.Flow;
import org.mule.routing.filters.WildcardFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.tck.junit4.FunctionalTestCase;

public class RestServiceComponentFlowTestCase extends FunctionalTestCase
{

    public static final String FLOW_NAME = "WORMS";
    public static final String FLOW_URL = MessageFormat.format("{0}header:serviceUrl{1}",
                                                                  ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                                                                  ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

    @Override
    protected String getConfigResources()
    {
        return "rest-service-component-test-flow.xml";
    }

    @Test
    public void testResetServiceNamespaceHandler() throws Exception
    {
        Flow f = (Flow) muleContext.getRegistry().lookupFlowConstruct(FLOW_NAME);
        
        Component component = (Component) f.getMessageProcessors().get(0);
        
        assertTrue(component instanceof RestServiceWrapper);
        RestServiceWrapper restServiceWrapper = (RestServiceWrapper) component;
        assertEquals(restServiceWrapper.getServiceUrl(), FLOW_URL);
        assertEquals(restServiceWrapper.getHttpMethod(), "POST");
        assertNotNull(restServiceWrapper.getFilter());
        assertEquals(NotFilter.class, restServiceWrapper.getFilter().getClass());
        NotFilter filter = (NotFilter) restServiceWrapper.getFilter();
        assertEquals(filter.getFilter().getClass(), WildcardFilter.class);
        WildcardFilter innerFilter = (WildcardFilter) filter.getFilter();
        assertEquals(innerFilter.getPattern(), "*xyz*");
        assertNotNull(restServiceWrapper.getPayloadParameterNames());
        assertEquals(restServiceWrapper.getPayloadParameterNames().size(), 2);
        assertEquals(restServiceWrapper.getPayloadParameterNames().get(0), "test-property1");
        assertEquals(restServiceWrapper.getPayloadParameterNames().get(1), "test-property2");

        assertNotNull(restServiceWrapper.getRequiredParams());
        assertEquals(restServiceWrapper.getRequiredParams().size(), 2);
        assertEquals(restServiceWrapper.getRequiredParams().get("r1"), "rv1");
        assertEquals(restServiceWrapper.getRequiredParams().get("r2"), "rv2");

        assertNotNull(restServiceWrapper.getOptionalParams());
        assertEquals(restServiceWrapper.getOptionalParams().size(), 2);
        assertEquals(restServiceWrapper.getOptionalParams().get("o1"), "ov1");
        assertEquals(restServiceWrapper.getOptionalParams().get("o2"), "ov2");
    }
}
