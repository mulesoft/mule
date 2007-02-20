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

import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.extras.spring.config.SpringConfigurationBuilder;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.NumberUtils;

import org.jbpm.msg.mule.Jbpm;

/**
 * Tests the connector against jBPM with a simple process which generates a Mule message.
 * jBPM is instantiated by Spring using the Spring jBpm module.
 */
public class MessagingJbpmSpringTestCase extends FunctionalTestCase {

    private ProcessConnector connector;
    private BPMS bpms;

    protected ConfigurationBuilder getBuilder() throws Exception {
        return new SpringConfigurationBuilder();
    }

    protected String getConfigResources() {
        return "jbpm-spring-config.xml";
    }

    protected void doPostFunctionalSetUp() throws Exception {
        connector =
            (ProcessConnector) MuleManager.getInstance().lookupConnector("jBpmConnector");
        bpms = connector.getBpms();
        connector.setAllowGlobalReceiver(true);
        super.doPostFunctionalSetUp();
    }

    public void testSendMessageProcess() throws Exception {
        // Deploy the process definition.
        ((Jbpm) bpms).deployProcess("sendMessageProcess.xml");

        UMOMessage response;
        Object process;
        BPMS bpms = connector.getBpms();
        MuleClient client = new MuleClient();
        try {
            // Create a new process.
            response = client.send("bpm://sendMessageProcess", "data", null);
            process = response.getPayload();

            long processId = NumberUtils.toLong(bpms.getId(process));
            // The process should be started and in a wait state.
            assertFalse(processId == -1);
            assertEquals("sendMessage", bpms.getState(process));

            // Advance the process one step.
            response = client.send("bpm://sendMessageProcess/" + processId, "data", null);
            process = response.getPayload();

            // The process should have ended.
            assertTrue(bpms.hasEnded(process));
        } finally {
            client.dispose();
        }
    }
}
