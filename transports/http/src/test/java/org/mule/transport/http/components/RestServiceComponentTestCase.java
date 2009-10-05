/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.components;

import org.mule.api.component.Component;
import org.mule.api.expression.ExpressionManager;
import org.mule.routing.filters.WildcardFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.tck.FunctionalTestCase;

import java.text.MessageFormat;

public class RestServiceComponentTestCase extends FunctionalTestCase
{

    public static final String SERVICE_NAME = "WORMS";
    public static final String SERVICE_URL = MessageFormat.format("{0}header:serviceUrl{1}",
                                                                  ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                                                                  ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

    protected String getConfigResources()
    {
        return "rest-service-component-test.xml";
    }

    public void testResetServiceNamespaceHandler() throws Exception
    {
        Component component = muleContext.getRegistry().lookupService(SERVICE_NAME).getComponent();
        assertTrue(component instanceof RestServiceWrapper);
        RestServiceWrapper restServiceWrapper = (RestServiceWrapper) component;
        assertEquals(restServiceWrapper.getServiceUrl(), SERVICE_URL);
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
