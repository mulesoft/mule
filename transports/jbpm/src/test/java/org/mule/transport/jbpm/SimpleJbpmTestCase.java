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
import org.mule.transport.bpm.ProcessConnector;

/**
 * Tests the connector against jBPM with a simple process.
 */
public class SimpleJbpmTestCase extends AbstractJbpmTestCase
{

    protected String getConfigResources()
    {
        return "jbpm-functional-test.xml";
    }

    public void testDeploy() throws Exception 
    {
        BPMS bpms = connector.getBpms();
        ((Jbpm) bpms).deployProcess("simple-process.jpdl.xml");

        Object process = bpms.startProcess("simple", null, null);
        assertNotNull(process);
        Object processId = bpms.getId(process);
        
        process = bpms.lookupProcess(processId);
        assertNotNull(process);        
    }
    
    public void testSimpleProcess() throws Exception {
        // Deploy the process definition.
        ((Jbpm) bpms).deployProcess("simple-process.jpdl.xml");

        MuleMessage response;
        Object process;
        BPMS bpms = connector.getBpms();
        MuleClient client = new MuleClient();
        try
        {
            // Create a new process.
            response = client.send("bpm://simple", "data", null);
            process = response.getPayload();

            String processId = (String)bpms.getId(process);
            // The process should be started and in a wait state.
            assertFalse(processId == null);
            assertEquals("dummyState", bpms.getState(process));

            // Advance the process one step.
            response = client.send("bpm://simple/" + processId, null, null);
            process = response.getPayload();

            // The process should have ended.
            assertTrue(bpms.hasEnded(process));
        }
        finally
        {
            client.dispose();
        }
    }

    public void testSimpleProcessWithParameters() throws Exception
    {
        // Deploy the process definition.
        ((Jbpm) bpms).deployProcess("simple-process.jpdl.xml");

        MuleMessage response;
        Object process;
        BPMS bpms = connector.getBpms();
        MuleClient client = new MuleClient();
        try
        {
            // Create a new process.
            response = client.send("bpm://?" +
                                   ProcessConnector.PROPERTY_ACTION + "=" + ProcessConnector.ACTION_START +
                                   "&" + ProcessConnector.PROPERTY_PROCESS_TYPE + "=simple", "data", null);
            process = response.getPayload();

            String processId =
                    response.getStringProperty(ProcessConnector.PROPERTY_PROCESS_ID, null);
            // The process should be started and in a wait state.
            assertFalse(processId == null);
            assertEquals("dummyState", bpms.getState(process));

            // Advance the process one step.
            response = client.send("bpm://?" +
                                   ProcessConnector.PROPERTY_ACTION + "=" + ProcessConnector.ACTION_ADVANCE +
                                   "&" + ProcessConnector.PROPERTY_PROCESS_TYPE + "=simple&" +
                                   ProcessConnector.PROPERTY_PROCESS_ID + "=" + processId, "data", null);
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
