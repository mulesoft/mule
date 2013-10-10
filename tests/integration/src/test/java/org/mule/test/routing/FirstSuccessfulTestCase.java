/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class FirstSuccessfulTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "first-successful-test.xml";
    }

    @Test
    public void testFirstSuccessful() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://input", "XYZ", null);
        assertEquals("XYZ is a string", response.getPayloadAsString());

        response = client.send("vm://input", Integer.valueOf(9), null);
        assertEquals("9 is an integer", response.getPayloadAsString());

        response = client.send("vm://input", Long.valueOf(42), null);
        assertEquals("42 is a number", response.getPayloadAsString());

        response = client.send("vm://input", Boolean.TRUE, null);
        assertNotNull(response);
        assertNotNull(response.getExceptionPayload());
        assertEquals(CouldNotRouteOutboundMessageException.class, response.getExceptionPayload().
                getException().
                getClass());
    }

    @Test
    public void testFirstSuccessfulWithExpression() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://input2", "XYZ", null);
        assertEquals("XYZ is a string", response.getPayloadAsString());
    }

    @Test
    public void testFirstSuccessfulWithExpressionAllFail() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("vm://input3", "XYZ", null);
        assertNotNull(response);
        assertNotNull(response.getExceptionPayload());
        assertEquals(CouldNotRouteOutboundMessageException.class, response.getExceptionPayload()
            .getException()
            .getClass());
    }

    @Test
    public void testFirstSuccessfulWithOneWayEndpoints() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://input4.in", TEST_MESSAGE, null);

        MuleMessage response = client.request("vm://output4.out", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals(TEST_MESSAGE, response.getPayload());
    }
}
