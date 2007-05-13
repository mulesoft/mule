/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm.jbpm;

import org.mule.config.ConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.extras.spring.config.SpringConfigurationBuilder;
import org.mule.providers.bpm.BPMS;
import org.mule.providers.bpm.tests.AbstractBpmTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.NumberUtils;

/**
 * Tests the connector against jBPM with a process which generates a Mule message and 
 * processes its response.
 * jBPM is instantiated by Spring using the Spring jBPM module.
 */
public class MessagingJbpmSpringTestCase extends AbstractBpmTestCase {

    protected ConfigurationBuilder getBuilder() throws Exception {
        return new SpringConfigurationBuilder();
    }

    protected String getConfigResources() {
        return "mule-jbpm-spring-config.xml";
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
