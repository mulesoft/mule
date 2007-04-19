/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm;

import org.mule.extras.client.MuleClient;
import org.mule.providers.bpm.jbpm.Jbpm;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.util.NumberUtils;

import org.jbpm.JbpmConfiguration;

/**
 * Tests the connector against jBPM with 2 simple processes.
 * jBPM is instantiated programatically via JbpmConfiguration.getInstance()
 */
public class SimpleJbpmTestCase extends FunctionalTestCase
{

    ProcessConnector connector;
    BPMS bpms;

    protected String getConfigResources()
    {
        return "jbpm-config.xml";
    }

    protected UMOManagementContext createManagementContext() throws Exception
    {
        UMOManagementContext context = super.createManagementContext();
        doSetupManager(context);
        return context;
    }

    protected void doSetupManager(UMOManagementContext context) throws Exception
    {
        // Configure the BPM connector programmatically so that we are able to pass it the jBPM instance.
        connector = new ProcessConnector();
        bpms = new Jbpm(JbpmConfiguration.getInstance());
        connector.setBpms(bpms);

        context.getRegistry().registerConnector(connector);
    }

    public void testSimpleProcess() throws Exception
    {
        // Deploy the process definition.
        ((Jbpm) bpms).deployProcess("dummyProcess.xml");

        UMOMessage response;
        Object process;
        BPMS bpms = connector.getBpms();
        MuleClient client = new MuleClient();
        try
        {
            // Create a new process.
            response = client.send("bpm://dummyProcess", "data", null);
            process = response.getPayload();

            long processId = NumberUtils.toLong(bpms.getId(process));
            // The process should be started and in a wait state.
            assertFalse(processId == -1);
            assertEquals("dummyState", bpms.getState(process));

            // Advance the process one step.
            response = client.send("bpm://dummyProcess/" + processId, null, null);
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
        ((Jbpm) bpms).deployProcess("dummyProcess.xml");

        UMOMessage response;
        Object process;
        BPMS bpms = connector.getBpms();
        MuleClient client = new MuleClient();
        try
        {
            // Create a new process.
            response = client.send("bpm://?" +
                    ProcessConnector.PROPERTY_ACTION + "=" + ProcessConnector.ACTION_START +
                    "&" + ProcessConnector.PROPERTY_PROCESS_TYPE + "=dummyProcess", "data", null);
            process = response.getPayload();

            long processId =
                    response.getLongProperty(ProcessConnector.PROPERTY_PROCESS_ID, -1);
            // The process should be started and in a wait state.
            assertFalse(processId == -1);
            assertEquals("dummyState", bpms.getState(process));

            // Advance the process one step.
            response = client.send("bpm://?" +
                    ProcessConnector.PROPERTY_ACTION + "=" + ProcessConnector.ACTION_ADVANCE +
                    "&" + ProcessConnector.PROPERTY_PROCESS_TYPE + "=dummyProcess&" +
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
