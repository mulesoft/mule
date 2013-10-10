/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.component.ComponentException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ChainingRouterNullsHandlingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/chaining-router-null-handling.xml";
    }

    @Test
    public void testNoComponentFails() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage message = new DefaultMuleMessage("thePayload", muleContext);
        MuleMessage result = muleClient.send("vm://incomingPass", message);
        assertNull("Shouldn't have any exceptions", result.getExceptionPayload());
        assertEquals("thePayload Received component1 Received component2Pass", result.getPayloadAsString());
    }

    @Test
    public void testLastComponentFails() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage message = new DefaultMuleMessage("thePayload", muleContext);
        MuleMessage result = muleClient.send("vm://incomingLastFail", message);
        assertNotNull("Should be a NullPayload instead.", result);
        assertEquals("Should be a NullPayload instead.", NullPayload.getInstance(), result.getPayload());
        assertNotNull("Should've contained an exception payload", result.getExceptionPayload());
        Throwable exception = result.getExceptionPayload().getException();
        assertNotNull("Exception required", exception);
        assertTrue("Wrong exception", exception instanceof ComponentException);
        Component component = ((ComponentException) exception).getComponent();
        assertEquals("Exception raised in wrong service", muleContext.getRegistry().lookupService(
            "component2Fail").getComponent(), component);
    }

    @Test
    public void testFirstComponentFails() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage message = new DefaultMuleMessage("thePayload", muleContext);
        MuleMessage result = muleClient.send("vm://incomingFirstFail", message);
        assertNotNull("Should be a NullPayload instead.", result);
        assertEquals("Should be a NullPayload instead.", NullPayload.getInstance(), result.getPayload());
        assertNotNull("Should've contained an exception payload", result.getExceptionPayload());
        Throwable exception = result.getExceptionPayload().getException();
        assertNotNull("Exception required", exception);
        assertTrue("Wrong exception", exception instanceof ComponentException);
        Component component = ((ComponentException) exception).getComponent();
        assertEquals("Exception raised in wrong service", muleContext.getRegistry().lookupService(
        "component1Fail").getComponent(), component);
    }

}
