/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.component.ComponentException;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;


/**
 */
public class ChainingRouterNullsHandlingTestCase extends FunctionalTestCase
{
    protected String getConfigResources() {
        return "org/mule/test/integration/routing/outbound/chaining-router-null-handling.xml";
    }

    public void testNoComponentFails() throws Exception {

        MuleClient muleClient = new MuleClient();
        MuleMessage result = muleClient.send("vm://incomingPass", new DefaultMuleMessage("thePayload"));
        assertNull("Shouldn't have any exceptions", result.getExceptionPayload());
        assertEquals("thePayload Received component1 Received component2Pass", result.getPayloadAsString());
    }

    public void testLastComponentFails() throws Exception {

        MuleClient muleClient = new MuleClient();
        MuleMessage result = muleClient.send("vm://incomingLastFail", new DefaultMuleMessage("thePayload"));
        assertNotNull("Should be a NullPayload instead.", result);
        assertEquals("Should be a NullPayload instead.", NullPayload.getInstance(), result.getPayload());
        assertNotNull("Should've contained an exception payload", result.getExceptionPayload());
        Throwable exception = result.getExceptionPayload().getException();
        assertNotNull("Exception required", exception);
        assertTrue("Wrong exception", exception instanceof ComponentException);
        String compName = ((ComponentException) exception).getComponent().getName();
        assertEquals("Exception raised in wrong component", "component2Fail", compName);
    }

    public void testFirstComponentFails() throws Exception {

        MuleClient muleClient = new MuleClient();
        MuleMessage result = muleClient.send("vm://incomingFirstFail", new DefaultMuleMessage("thePayload"));
        assertNotNull("Should be a NullPayload instead.", result);
        assertEquals("Should be a NullPayload instead.", NullPayload.getInstance(), result.getPayload());
        assertNotNull("Should've contained an exception payload", result.getExceptionPayload());
        Throwable exception = result.getExceptionPayload().getException();
        assertNotNull("Exception required", exception);
        assertTrue("Wrong exception", exception instanceof ComponentException);
        String compName = ((ComponentException) exception).getComponent().getName();
        assertEquals("Exception raised in wrong component", "component1Fail", compName);
    }
}
