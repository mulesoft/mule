/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jbpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.bpm.BPMS;
import org.mule.module.bpm.Process;
import org.mule.tck.AbstractServiceAndFlowTestCase;

/**
 * Tests jBPM component with a simple process. The ProcessEngine is built by Spring
 * and injected into the Mule wrapper.
 */
public class SpringJbpmComponentTestCase extends AbstractServiceAndFlowTestCase
{

    public SpringJbpmComponentTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "spring-jbpm-component-service.xml"},
            {ConfigVariant.FLOW, "spring-jbpm-component-flow.xml"}});
    }

    @Test
    public void testSimpleProcess() throws Exception
    {
        MuleClient client = muleContext.getClient();
        BPMS bpms = muleContext.getRegistry().lookupObject(BPMS.class);
        assertNotNull(bpms);

        // Create a new process.
        MuleMessage response = client.send("vm://simple", "data", null);
        Object process = response.getPayload();

        String processId = (String) bpms.getId(process);
        // The process should be started and in a wait state.
        assertFalse(processId == null);
        assertEquals("dummyState", bpms.getState(process));

        // Advance the process one step.
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Process.PROPERTY_PROCESS_ID, processId);
        response = client.send("vm://simple", null, props);
        process = response.getPayload();

        // The process should have ended.
        assertTrue(bpms.hasEnded(process));
    }

}
