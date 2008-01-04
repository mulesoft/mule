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

import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

import javax.jms.TextMessage;

public class ReplytoChainIntegration1TestCase extends FunctionalTestCase
{
    public ReplytoChainIntegration1TestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/replyto/replyto-chain-integration-test-1.xml";
    }

    public void testReplyToChain() throws Exception
    {
        String message = "test";

        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");
        UMOMessage result = client.send("vm://pojo1", message, props);
        assertNotNull(result);
        // TODO This assertion is incorrect a "TextMessage" should not be received here but
        // rather just a sting payload.  See MULE-2869
        assertEquals("Received: " + message, ((TextMessage)result.getPayload()).getText());
    }
    
    public void testReplyToChainWithoutProps() throws Exception
    {
        String message = "test";

        MuleClient client = new MuleClient();
        UMOMessage result = client.send("vm://pojo1", message, null);
        assertNotNull(result);
        // TODO This assertion is incorrect a "TextMessage" should not be received here but
        // rather just a sting payload.  See MULE-2869
        assertEquals("Received: " + message, ((TextMessage)result.getPayload()).getText());
    }

}
