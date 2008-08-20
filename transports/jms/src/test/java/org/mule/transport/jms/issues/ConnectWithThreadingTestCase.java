/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.issues;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ConnectWithThreadingTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "jms_connect_with_threading.xml";
    }
    
    public void testConnectWithThreading() throws Exception 
    { 
        MuleClient client = new MuleClient(); 
        //        client.dispatch("jms://in", "Test", null); 
        //        UMOMessage msg = client.receive("jms://out", 10000); 

        // needs to wait for the jms connection to be established 
        Thread.sleep(1000); 
        MuleMessage msg = client.send("jms://in", new DefaultMuleMessage("Test")); 

        //TODO: Still more Threading issues when using SimpleReconnectingStrategy with doThreading=true       
        assertNotNull(msg.getPayload()); 
        assertEquals("Test", msg.getPayloadAsString()); 

        client.dispose(); 
    }
    
}


