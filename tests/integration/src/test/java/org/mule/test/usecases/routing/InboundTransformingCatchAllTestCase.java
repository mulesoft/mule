/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.routing;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

/*
 * In this Test Case we make use of a Custom Catch All Strategy in order to show how
 * to send the transformed message instead of the non-transformed message.
 */
public class InboundTransformingCatchAllTestCase extends FunctionalTestCase
{

    public void testNormal() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://in1", new DefaultMuleMessage("HELLO!", muleContext));
        MuleMessage msg = client.request("vm://catchall", 3000);
        assertNotNull(msg);
        assertTrue(msg.getPayload() instanceof String);

        client.dispatch("vm://in2", new DefaultMuleMessage("HELLO!", muleContext));
        msg = client.request("vm://catchall", 3000);
        assertNotNull(msg);
        assertTrue(msg.getPayload() instanceof byte[]);
    }

    protected String getConfigResources()
    {
        return "org/mule/test/usecases/routing/inbound-transforming-catchall.xml";
    }
}
