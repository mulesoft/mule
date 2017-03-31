/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.core.lifecycle.LifecycleTrackerProcessor.FLOW_CONSRUCT_PROPERTY;
import static org.mule.test.core.lifecycle.LifecycleTrackerProcessor.LIFECYCLE_TRACKER_PROCESSOR_PROPERTY;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class SubFlowTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/sub-flow.xml";
  }

  @Test
  public void testProcessorChainViaProcessorRef() throws Exception {
    Event result = flowRunner("ProcessorChainViaProcessorRef").withPayload("").run();
    assertThat(result.getMessageAsString(muleContext), is("1xyz2"));

    assertThat(((InternalMessage) result.getMessage()).getOutboundProperty(LIFECYCLE_TRACKER_PROCESSOR_PROPERTY),
               is("[setMuleContext, setService, initialise, start]"));
    assertThat(result.getVariable(FLOW_CONSRUCT_PROPERTY).getValue(),
               is(muleContext.getRegistry().lookupFlowConstruct("ProcessorChainViaProcessorRef")));
  }

  @Test
  public void testProcessorChainViaFlowRef() throws Exception {
    Event result = flowRunner("ProcessorChainViaFlowRef").withPayload("").run();
    assertThat(result.getMessageAsString(muleContext), is("1xyz2"));

    assertThat(((InternalMessage) result.getMessage()).getOutboundProperty(LIFECYCLE_TRACKER_PROCESSOR_PROPERTY),
               is("[setMuleContext, setService, initialise, start]"));
    assertThat(result.getVariable(FLOW_CONSRUCT_PROPERTY).getValue(),
               is(muleContext.getRegistry().lookupFlowConstruct("ProcessorChainViaFlowRef")));
  }

  @Test
  public void testSubFlowViaProcessorRef() throws Exception {
    Event result = flowRunner("SubFlowViaProcessorRef").withPayload("").run();
    assertThat(result.getMessageAsString(muleContext), is("1xyz2"));

    assertThat(((InternalMessage) result.getMessage()).getOutboundProperty(LIFECYCLE_TRACKER_PROCESSOR_PROPERTY),
               is("[setMuleContext, setService, initialise, start]"));
    assertThat(result.getVariable(FLOW_CONSRUCT_PROPERTY).getValue(),
               is(muleContext.getRegistry().lookupFlowConstruct("SubFlowViaProcessorRef")));
  }

  @Test
  public void testSubFlowViaFlowRef() throws Exception {
    Event result = flowRunner("SubFlowViaFlowRef").withPayload("").run();
    assertThat(result.getMessageAsString(muleContext), is("1xyz2"));

    assertThat(((InternalMessage) result.getMessage()).getOutboundProperty(LIFECYCLE_TRACKER_PROCESSOR_PROPERTY),
               is("[setMuleContext, setService, initialise, start]"));
    assertThat(result.getVariable(FLOW_CONSRUCT_PROPERTY).getValue(),
               is(muleContext.getRegistry().lookupFlowConstruct("SubFlowViaFlowRef")));
  }

  @Test
  public void testFlowviaFlowRef() throws Exception {
    assertThat(getPayloadAsString(flowRunner("FlowViaFlowRef").withPayload("").run().getMessage()), is("1xyz2"));
  }

  @Test
  public void testServiceviaFlowRef() throws Exception {
    assertThat(getPayloadAsString(flowRunner("ServiceViaFlowRef").withPayload("").run().getMessage()), is("1xyz2"));
  }

  @Test
  public void testFlowWithSubFlowWithComponent() throws Exception {
    assertThat(getPayloadAsString(flowRunner("flowWithsubFlowWithComponent").withPayload("0").run().getMessage()), is("0"));

  }

  @Test
  public void testFlowWithSameSubFlowTwice() throws Exception {
    assertThat(getPayloadAsString(flowRunner("flowWithSameSubFlowTwice").withPayload("0").run().getMessage()), is("0xyzxyz"));
  }

  @Test
  public void testFlowWithSameSubFlowSingletonTwice() throws Exception {
    assertThat(getPayloadAsString(flowRunner("flowWithSameSubFlowSingletonTwice").withPayload("0").run().getMessage()),
               is("0xyzxyz"));
  }

  @Test
  public void testFlowWithSameGlobalChainTwice() throws Exception {
    assertThat(getPayloadAsString(flowRunner("flowWithSameGlobalChainTwice").withPayload("0").run().getMessage()), is("0xyzxyz"));
  }

  @Test
  public void testFlowWithSameGlobalChainSingletonTwice() throws Exception {
    assertThat(getPayloadAsString(flowRunner("flowWithSameGlobalChainSingletonTwice").withPayload("0").run().getMessage()),
               is("0xyzxyz"));
  }

}
