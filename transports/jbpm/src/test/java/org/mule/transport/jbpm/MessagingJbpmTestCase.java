/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jbpm;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.bpm.BPMS;

/**
 * Tests the connector against jBPM with a process which generates 
 * a Mule message and processes its response. jBPM is instantiated by Spring. 
 */
public class MessagingJbpmTestCase extends AbstractJbpmTestCase
{
	static {
	   	System.setProperty( PROPERTY_MULE_TEST_TIMEOUT, "300");
	}
	
    protected String getConfigResources()
    {
        return "jbpm-functional-test.xml";
    }

    public void testSendMessageProcess() throws Exception
    {
        MuleMessage response;
        Object process;
        BPMS bpms = connector.getBpms();
        MuleClient client = new MuleClient();
        try
        {
            // Create a new process.
        	response = client.send("bpm://message", "data", null);                  	
            process = response.getPayload();
            assertTrue(bpms.isProcess(process)); 

            String processId = (String)bpms.getId(process);
            // The process should have sent a synchronous message, followed by an asynchronous message and now be in a wait state.
            assertFalse(processId == null);
            assertEquals("waitForResponse", bpms.getState(process));

            // Advance the process one step.
            response = client.send("bpm://message/" + processId, "data", null);
            process = response.getPayload();

            // The process should have ended.
            assertTrue(bpms.hasEnded(process));
        }
        finally
        {
            client.dispose();
        }
    }
}
