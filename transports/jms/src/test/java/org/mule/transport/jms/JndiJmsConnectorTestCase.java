/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.jms.integration.AbstractJmsFunctionalTestCase;

public class JndiJmsConnectorTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "jms-jndi-config.xml";
    }

    public void testConnectionFactoryFromJndi() throws Exception
    {
        MuleClient client = new MuleClient();

        client.dispatch("jms://in1?connector=jmsConnector1", DEFAULT_INPUT_MESSAGE, null);


        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, result.getPayloadAsString());
    } 
 
    public void testQueueFromJndi() throws Exception
    {
        MuleClient client = new MuleClient();

        client.dispatch("jms://in2?connector=jmsConnector2", DEFAULT_INPUT_MESSAGE, null);
        
        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, result.getPayloadAsString());
    }
    
    public void testTopicFromJndi() throws Exception
    {
        MuleClient client = new MuleClient();
        
        client.dispatch("jms://topic:in3?connector=jmsConnector2", DEFAULT_INPUT_MESSAGE, null);
        
        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, result.getPayloadAsString());
    }
    
}
