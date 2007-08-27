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

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.ArrayList;
import java.util.List;

public class ForwardingMessageSplitterTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/routing/forwarding-message-splitter.xml";
    }

    public void testSyncResponse() throws Exception
    {
        MuleClient client = new MuleClient();

        List payload = new ArrayList();
        payload.add("hello");
        payload.add(new Integer(3));
        payload.add(new Exception());
        client.send("vm://in.queue", payload, null);
        UMOMessage m = client.receive("vm://component.1", 2000);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        m = client.receive("vm://component.2", 2000);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof Integer);

        m = client.receive("vm://error.queue", 2000);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof Exception);
    }
}
