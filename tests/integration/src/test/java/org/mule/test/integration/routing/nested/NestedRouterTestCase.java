/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.nested;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class NestedRouterTestCase extends FunctionalTestCase
{
    private static final int RECEIVE_TIMEOUT = 5000;

    private static final int number = 0xC0DE;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/nested/nestedrouter-test.xml";
    }

    private void internalTest(String prefix) throws Exception
    {
        MuleClient client = new MuleClient();
        String message = "Mule";
        client.dispatch(prefix + "invoker.in", message, null);
        UMOMessage reply = client.receive(prefix + "invoker.out", RECEIVE_TIMEOUT);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        assertEquals("Received: Hello " + message + " " + number, reply.getPayload());
    }

    public void testNestedRouter() throws Exception
    {
        internalTest("vm://");
    }

    public void testJmsQueueNestedRouter() throws Exception
    {
        internalTest("jms://");
    }

    public void testJmsTopicNestedRouter() throws Exception
    {
        internalTest("jms://topic:t");
    }
}
