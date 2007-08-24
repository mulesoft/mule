/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.replyto;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class ReplytoChainIntegration4TestCase extends FunctionalTestCase
{
    public ReplytoChainIntegration4TestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/replyto/routing-chain-4-test.xml";
    }

    public void testReplyToChain() throws Exception
    {
        String message = "test";

        MuleClient client = new MuleClient();
        UMOMessage result = client.send("vm://pojo1", message, null);
        assertNotNull(result);
        assertEquals("Received: " + message, result.getPayload());
    }
}
