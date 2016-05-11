/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.component.Component;
import org.mule.api.expression.ExpressionManager;
import org.mule.construct.Flow;
import org.mule.routing.filters.WildcardFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import java.text.MessageFormat;

import org.junit.Test;

public class RestServiceComponentFlowTestCase extends FunctionalTestCase
{
    public static final String FLOW_NAME = "WORMS";
    public static final String FLOW_URL = MessageFormat.format("{0}header:serviceUrl{1}",
                                                                  ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                                                                  ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

    @Override
    protected String getConfigFile()
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
