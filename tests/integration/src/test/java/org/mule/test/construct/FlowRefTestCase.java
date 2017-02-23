/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class FlowRefTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  private static String FLOW1_SENSING_PROCESSOR_NAME = "NonBlockingFlow1SensingProcessor";
  private static String FLOW2_SENSING_PROCESSOR_NAME = "NonBlockingFlow2SensingProcessor";
  private static String TO_SYNC_FLOW1_SENSING_PROCESSOR_NAME = "NonBlockingToSyncFlow1SensingProcessor";
  private static String TO_SYNC_FLOW2_SENSING_PROCESSOR_NAME = "NonBlockingToSyncFlow2SensingProcessor";
  private static String ERROR_MESSAGE = "ERROR";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-ref.xml";
  }

  @Before
  public void before() {
    ProcessorPathAssertingProcessor.traversedProcessorPaths.clear();
  }

  @Test
  public void twoFlowRefsToSubFlow() throws Exception {
    final Event muleEvent = flowRunner("flow1").withPayload("0").run();
    assertThat(getPayloadAsString(muleEvent.getMessage()), is("012xyzabc312xyzabc3"));
  }

  @Test
  public void dynamicFlowRef() throws Exception {
    assertEquals("0A",
                 flowRunner("flow2").withPayload("0").withVariable("letter", "A").run().getMessageAsString(muleContext));
    assertEquals("0B",
                 flowRunner("flow2").withPayload("0").withVariable("letter", "B").run().getMessageAsString(muleContext));
  }

  public static class ProcessorPathAssertingProcessor implements Processor, FlowConstructAware {

    private static List<String> traversedProcessorPaths = new ArrayList<>();
    private FlowConstruct flowConstruct;

    @Override
    public Event process(Event event) throws MuleException {
      traversedProcessorPaths
          .add(((Flow) muleContext.getRegistry().lookupFlowConstruct(flowConstruct.getName())).getProcessorPath(this));
      return event;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct) {
      this.flowConstruct = flowConstruct;
    }
  }

  @Ignore("MULE-11482 - ignoring since it's going to be removed on next commit")
  @Test
  public void dynamicFlowRefProcessorPath() throws Exception {
    flowRunner("flow2").withPayload("0").withVariable("letter", "J").run();

    assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.size(), is(1));
    assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(0),
               is("/flow2/processors/0/sub-flow-J/subprocessors/0"));
  }

  @Ignore("MULE-11482 - ignoring since it's going to be removed on next commit")
  @Test
  public void dynamicFlowRefProcessorPathSameSubflowFromSingleFlow() throws Exception {
    flowRunner("flow3").withPayload("0").withVariable("letter", "J").run();

    assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.size(), is(2));
    assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(0),
               is("/flow3/processors/0/sub-flow-J/subprocessors/0"));
    assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(1),
               is("/flow3/processors/1/sub-flow-J/subprocessors/0"));
  }

  @Ignore("MULE-11482 - ignoring since it's going to be removed on next commit")
  @Test
  public void dynamicFlowRefProcessorPathSameSubflowFromDifferentFlow() throws Exception {
    flowRunner("flow2").withPayload("0").withVariable("letter", "J").run();

    flowRunner("flow3").withPayload("0").withVariable("letter", "J").run();

    assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.size(), is(3));
    assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(0),
               is("/flow2/processors/0/sub-flow-J/subprocessors/0"));
    assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(1),
               is("/flow3/processors/0/sub-flow-J/subprocessors/0"));
    assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(2),
               is("/flow3/processors/1/sub-flow-J/subprocessors/0"));
  }

  @Test
  public void dynamicFlowRefWithChoice() throws Exception {
    assertEquals("0A",
                 flowRunner("flow2").withPayload("0").withVariable("letter", "C").run().getMessageAsString(muleContext));
  }

  @Test
  public void dynamicFlowRefWithScatterGather() throws Exception {
    List<InternalMessage> messageList =
        (List<InternalMessage>) flowRunner("flow2").withPayload("0").withVariable("letter", "SG").run().getMessage()
            .getPayload().getValue();

    List payloads = messageList.stream().map(msg -> msg.getPayload().getValue()).collect(toList());
    assertEquals("0A", payloads.get(0));
    assertEquals("0B", payloads.get(1));
  }

  @Test(expected = MessagingException.class)
  public void flowRefNotFound() throws Exception {
    assertEquals("0C",
                 flowRunner("flow2").withPayload("0").withVariable("letter", "Z").run().getMessageAsString(muleContext));
  }

}
