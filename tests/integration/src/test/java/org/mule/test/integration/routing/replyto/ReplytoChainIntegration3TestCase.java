/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.replyto;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class ReplytoChainIntegration3TestCase extends FunctionalTestCase
{
    public ReplytoChainIntegration3TestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/replyto/routing-chain-3-test.xml";
    }

    public void testReplyToChain() throws Exception
    {
        String message = "test";

        MuleClient client = new MuleClient();
        client.dispatch("vm://pojo1", message, null);
        UMOMessage result = client.receive("jms://response", 10000);
        assertNotNull(result);
        assertEquals("Received: " + message, result.getPayload());
    }
}
