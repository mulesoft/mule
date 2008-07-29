/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

public class JmsQueueWithTransactionTestCase extends AbstractJmsFunctionalTestCase
{
    
    protected String getConfigResources()
    {
        return "providers/activemq/jms-queue-with-transaction.xml";
    }

    public void testOutboundJmsTransaction() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://in", new DefaultMuleMessage(DEFAULT_INPUT_MESSAGE));
            
        MuleMessage response = client.request("vm://out", TIMEOUT);
        assertNotNull(response);
        assertEquals(DEFAULT_INPUT_MESSAGE, response.getPayloadAsString());
    }
    
}
