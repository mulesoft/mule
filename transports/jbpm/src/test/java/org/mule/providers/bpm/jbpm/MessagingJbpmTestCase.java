/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm.jbpm;

import org.mule.extras.client.MuleClient;
import org.mule.providers.bpm.BPMS;
import org.mule.umo.UMOMessage;
import org.mule.util.NumberUtils;

/**
 * Tests the connector against jBPM with a process which generates a Mule message and 
 * processes its response.
 * jBPM is instantiated by Spring using the Spring jBPM module.
 */
public class MessagingJbpmTestCase extends AbstractJbpmTestCase {

    protected String getConfigResources() {
        return "jbpm-functional-test.xml";
    }

    public void testSendMessageProcess() throws Exception {
        // Deploy the process definition.
        ((Jbpm) bpms).deployProcess("message-process.xml");

        UMOMessage response;
        Object process;
        BPMS bpms = connector.getBpms();
        MuleClient client = new MuleClient();
        try {
            // Create a new process.
            response = client.send("bpm://message", "data", null);
            process = response.getPayload();

            long processId = NumberUtils.toLong(bpms.getId(process));
            // The process should be started and in a wait state.
            assertFalse(processId == -1);
            assertEquals("sendMessage", bpms.getState(process));

            // Advance the process one step.
            response = client.send("bpm://message/" + processId, "data", null);
            process = response.getPayload();

            // The process should have ended.
            assertTrue(bpms.hasEnded(process));
        } finally {
            client.dispose();
        }
    }
}
