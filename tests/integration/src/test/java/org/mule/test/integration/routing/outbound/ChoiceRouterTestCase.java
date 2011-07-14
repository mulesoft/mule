/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.outbound;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ChoiceRouterTestCase extends FunctionalTestCase
{
    private static final String WITH_DEFAULT_ROUTE_CHANNEL = "vm://with-default-route.in";
    private static final String WITHOUT_DEFAULT_ROUTE_CHANNEL = "vm://without-default-route.in";

    private MuleClient muleClient;
    
    public ChoiceRouterTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/choice-router-test.xml";
    }

    @Test
    public void testNoRouteFound() throws Exception
    {
        final MuleMessage result = muleClient.send(WITHOUT_DEFAULT_ROUTE_CHANNEL, "bad", null);
        assertNotNull(result);
        assertNotNull("should have got a MuleException", result.getExceptionPayload());
        assertNotNull(result.getExceptionPayload().getException() instanceof MuleException);
        assertNotNull(result.getExceptionPayload().getRootException() instanceof RoutePathNotFoundException);
    }

    @Test
    public void testRoutesFound() throws Exception
    {
        String result = muleClient.send(WITHOUT_DEFAULT_ROUTE_CHANNEL, "apple", null).getPayloadAsString();
        assertEquals("apple:fruit:fruit", result);

        result = muleClient.send(WITH_DEFAULT_ROUTE_CHANNEL, "apple", null).getPayloadAsString();
        assertEquals("apple:fruit:fruit", result);

        result = muleClient.send(WITH_DEFAULT_ROUTE_CHANNEL, "turnip", null).getPayloadAsString();
        assertEquals("turnip:veggie:veggie", result);
    }

    @Test
    public void testWhenExpressionRouteFound() throws Exception
    {
        final String result = muleClient.send(WITH_DEFAULT_ROUTE_CHANNEL, "blueberry", null)
            .getPayloadAsString();
        assertEquals("blueberry:fruit:fruit", result);
    }

    @Test
    public void testDefaultRoute() throws Exception
    {
        final String result = muleClient.send(WITH_DEFAULT_ROUTE_CHANNEL, "car", null).getPayloadAsString();
        assertEquals("car:default:default", result);
    }
}
