/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ChainingRouterNullsHandlingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/chaining-router-null-handling-service.xml";
    }

    @Test
    public void testNoComponentFails() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage("thePayload", muleContext);
        MuleMessage result = client.send("vm://incomingPass", message);
        assertNull("Shouldn't have any exceptions", result.getExceptionPayload());
        assertEquals("thePayload Received component1 Received component2Pass", result.getPayloadAsString());
    }

    @Test
    public void testLastComponentFails() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("thePayload", muleContext);

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://incomingLastFail", message);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(Component2Exception.class, result.getExceptionPayload().getRootException().getClass());
    }


    @Test
    public void testFirstComponentFails() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("thePayload", muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://incomingFirstFail", message);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(Component1Exception.class, result.getExceptionPayload().getRootException().getClass());
    }
}
