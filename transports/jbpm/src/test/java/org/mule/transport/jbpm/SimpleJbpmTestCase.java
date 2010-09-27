/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jbpm;

import org.mule.api.MuleMessage;
import org.mule.module.bpm.BPMS;
import org.mule.module.bpm.Process;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.bpm.ProcessConnector;

/**
 * Tests the connector against jBPM with a simple process.
 * 
 * @deprecated It is recommended to configure BPM as a component rather than a transport for 3.x
 */
public class SimpleJbpmTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "jbpm-functional-test.xml";
    }

    public void testSimpleProcess() throws Exception 
    {
        ProcessConnector connector = (ProcessConnector) muleContext.getRegistry().lookupConnector("bpmConnector");
        BPMS bpms = connector.getBpms();
        assertNotNull(bpms);

        MuleClient client = new MuleClient(muleContext);
        try
        {
            // Create a new process.
            MuleMessage response = client.send("bpm://simple", "data", null);
            Object process = response.getPayload();

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
        ProcessConnector connector = (ProcessConnector) muleContext.getRegistry().lookupConnector("bpmConnector");
        BPMS bpms = connector.getBpms();

        MuleClient client = new MuleClient(muleContext);
        try
        {
            // Create a new process.
            MuleMessage response = client.send("bpm://?" +
                                   Process.PROPERTY_ACTION + "=" + Process.ACTION_START +
                                   "&" + Process.PROPERTY_PROCESS_TYPE + "=simple", "data", null);
            Object process = response.getPayload();

            // The process should be started and in a wait state.
            Object processId = bpms.getId(process);
            assertNotNull(processId);
            assertEquals("dummyState", bpms.getState(process));

            // Advance the process one step.
            response = client.send("bpm://?" +
                                   Process.PROPERTY_ACTION + "=" + Process.ACTION_ADVANCE +
                                   "&" + Process.PROPERTY_PROCESS_TYPE + "=simple&" +
                                   Process.PROPERTY_PROCESS_ID + "=" + processId, "data", null);
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
