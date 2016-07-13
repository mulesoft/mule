/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.lifecycle.LifecycleTrackerProcessor;

import org.junit.Test;

public class SubFlowTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/sub-flow.xml";
    }

    @Test
    public void testProcessorChainViaProcessorRef() throws Exception
    {
        MuleEvent result = flowRunner("ProcessorChainViaProcessorRef").withPayload("").run();
        assertEquals("1xyz2", result.getMessageAsString());

        assertEquals("[setMuleContext, setService, setMuleContext, initialise, start]",
            result.getMessage().getOutboundProperty(LifecycleTrackerProcessor.LIFECYCLE_TRACKER_PROCESSOR_PROPERTY));
        assertEquals(muleContext.getRegistry().lookupFlowConstruct("ProcessorChainViaProcessorRef"),
            result.getFlowVariable(LifecycleTrackerProcessor.FLOW_CONSRUCT_PROPERTY));
    }

    @Test
    public void testProcessorChainViaFlowRef() throws Exception
    {
        MuleEvent result = flowRunner("ProcessorChainViaFlowRef").withPayload("").run();

        assertEquals("1xyz2", result.getMessageAsString());

        assertEquals("[setMuleContext, setService, setMuleContext, initialise, start]",
            result.getMessage().getOutboundProperty(LifecycleTrackerProcessor.LIFECYCLE_TRACKER_PROCESSOR_PROPERTY));
        assertEquals(muleContext.getRegistry().lookupFlowConstruct("ProcessorChainViaFlowRef"),
            result.getFlowVariable(LifecycleTrackerProcessor.FLOW_CONSRUCT_PROPERTY));
    }
    
    @Test
    public void testSubFlowViaProcessorRef() throws Exception
    {
        MuleEvent result = flowRunner("SubFlowViaProcessorRef").withPayload("").run();
        assertEquals("1xyz2", result.getMessageAsString());

        assertEquals("[setMuleContext, setService, setMuleContext, initialise, start]",
            result.getMessage().getOutboundProperty(LifecycleTrackerProcessor.LIFECYCLE_TRACKER_PROCESSOR_PROPERTY));
        assertEquals(muleContext.getRegistry().lookupFlowConstruct("SubFlowViaProcessorRef"),
            result.getFlowVariable(LifecycleTrackerProcessor.FLOW_CONSRUCT_PROPERTY));
    }

    @Test
    public void testSubFlowViaFlowRef() throws Exception
    {
        MuleEvent result = flowRunner("SubFlowViaFlowRef").withPayload("").run();

        assertEquals("1xyz2", result.getMessageAsString());

        assertEquals("[setMuleContext, setService, setMuleContext, initialise, start]",
            result.getMessage().getOutboundProperty(LifecycleTrackerProcessor.LIFECYCLE_TRACKER_PROCESSOR_PROPERTY));
        assertEquals(muleContext.getRegistry().lookupFlowConstruct("SubFlowViaFlowRef"),
            result.getFlowVariable(LifecycleTrackerProcessor.FLOW_CONSRUCT_PROPERTY));
    }

    @Test
    public void testFlowviaFlowRef() throws Exception
    {
        assertEquals("1xyz2", getPayloadAsString(flowRunner("FlowViaFlowRef").withPayload("").run().getMessage()));
    }

    @Test
    public void testServiceviaFlowRef() throws Exception
    {
        assertEquals("1xyz2", getPayloadAsString(flowRunner("ServiceViaFlowRef").withPayload("").run().getMessage()));
    }

    @Test
    public void testFlowWithSubFlowWithComponent() throws Exception
    {
        assertEquals("0", getPayloadAsString(flowRunner("flowWithsubFlowWithComponent").withPayload("0").run().getMessage()));

    }

    @Test
    public void testFlowWithSameSubFlowTwice() throws Exception
    {
        assertEquals("0xyzxyz", getPayloadAsString(flowRunner("flowWithSameSubFlowTwice").withPayload("0").run().getMessage()));
    }

    @Test
    public void testFlowWithSameSubFlowSingletonTwice() throws Exception
    {
        assertEquals("0xyzxyz", getPayloadAsString(flowRunner("flowWithSameSubFlowSingletonTwice").withPayload("0").run().getMessage()));
    }

    @Test
    public void testFlowWithSameGlobalChainTwice() throws Exception
    {
        assertEquals("0xyzxyz", getPayloadAsString(flowRunner("flowWithSameGlobalChainTwice").withPayload("0").run().getMessage()));
    }

    @Test
    public void testFlowWithSameGlobalChainSingletonTwice() throws Exception
    {
        assertEquals("0xyzxyz", getPayloadAsString(flowRunner("flowWithSameGlobalChainSingletonTwice").withPayload("0").run().getMessage()));
    }

}
