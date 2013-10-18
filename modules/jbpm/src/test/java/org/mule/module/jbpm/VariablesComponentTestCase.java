/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jbpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.bpm.BPMS;
import org.mule.module.bpm.Process;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class VariablesComponentTestCase extends AbstractServiceAndFlowTestCase
{

    private final int TIMEOUT = 10000;

    public VariablesComponentTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "jbpm-component-functional-test-service.xml"},
            {ConfigVariant.FLOW, "jbpm-component-functional-test-flow.xml"}
        });
    }      
    
    @Test
    public void testVariables() throws Exception
    {
        MuleClient client = muleContext.getClient();
        BPMS bpms = muleContext.getRegistry().lookupObject(BPMS.class);
        assertNotNull(bpms);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "bar");
        MuleMessage response = client.send("vm://variables", "data", props);
        String processId = (String)bpms.getId(response.getPayload());
        assertNotNull(processId);

        response = client.request("vm://queueA", TIMEOUT);
        assertNotNull(response);
        assertEquals("bar", response.getInboundProperty("foo"));
        assertEquals(0.75, response.getInboundProperty("fraction"));

        // Advance the process
        props = new HashMap<String, Object>();
        props.put(Process.PROPERTY_PROCESS_ID, processId);
        props.put("straw", "berry");
        props.put("time", new Date());
        response = client.send("vm://variables", "data", props);
        
        response = client.request("vm://queueB", TIMEOUT);
        assertNotNull(response);
        assertEquals("bar", response.getInboundProperty("foo"));
        assertEquals(0.75, response.getInboundProperty("fraction"));
        assertEquals("berry", response.getInboundProperty("straw"));
        final Object o = response.getInboundProperty("time");
        assertTrue(o instanceof Date);
    }

}
