/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
