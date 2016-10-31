/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.DynamicPipelineException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.transformer.simple.ParseTemplateTransformer;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DynamicFlowTestCase extends AbstractIntegrationTestCase {

  private MuleClient client;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/dynamic-flow.xml";
  }

  @Before
  public void before() {
    client = muleContext.getClient();
  }

  @Test
  public void addPreMessageProccesor() throws Exception {
    Event muleEvent = flowRunner("dynamicFlow").withPayload("source->").run();
    InternalMessage result = muleEvent.getMessage();
    assertEquals("source->(static)", getPayloadAsString(result));

    Flow flow = getFlow("dynamicFlow");
    String pipelineId = flow.dynamicPipeline(null).injectBefore(new StringAppendTransformer("(pre)")).resetAndUpdate();
    muleEvent = flowRunner("dynamicFlow").withPayload("source->").run();
    result = muleEvent.getMessage();
    assertEquals("source->(pre)(static)", getPayloadAsString(result));

    flow.dynamicPipeline(pipelineId).injectBefore(new StringAppendTransformer("(pre1)"), new StringAppendTransformer("(pre2)"))
        .resetAndUpdate();
    muleEvent = flowRunner("dynamicFlow").withPayload("source->").run();
    result = muleEvent.getMessage();
    assertEquals("source->(pre1)(pre2)(static)", getPayloadAsString(result));
  }

  @Test
  public void addPrePostMessageProccesor() throws Exception {
    Flow flow = getFlow("dynamicFlow");
    String pipelineId = flow.dynamicPipeline(null).injectBefore(new StringAppendTransformer("(pre)"))
        .injectAfter(new StringAppendTransformer("(post)")).resetAndUpdate();
    Event muleEvent = flowRunner("dynamicFlow").withPayload("source->").run();
    InternalMessage result = muleEvent.getMessage();
    assertEquals("source->(pre)(static)(post)", getPayloadAsString(result));

    flow.dynamicPipeline(pipelineId).injectBefore(new StringAppendTransformer("(pre)"))
        .injectAfter(new StringAppendTransformer("(post1)"), new StringAppendTransformer("(post2)")).resetAndUpdate();
    muleEvent = flowRunner("dynamicFlow").withPayload("source->").run();
    result = muleEvent.getMessage();
    assertEquals("source->(pre)(static)(post1)(post2)", getPayloadAsString(result));
  }

  @Test
  public void dynamicComponent() throws Exception {
    // invocation #1
    InternalMessage result = flowRunner("dynamicComponentFlow").withPayload("source->").run().getMessage();
    assertEquals("source->(static)", getPayloadAsString(result));

    // invocation #2
    result = flowRunner("dynamicComponentFlow").withPayload("source->").run().getMessage();
    assertEquals("source->chain update #1(static)", getPayloadAsString(result));

    // invocation #3
    result = flowRunner("dynamicComponentFlow").withPayload("source->").run().getMessage();
    assertEquals("source->chain update #2(static)", getPayloadAsString(result));
  }

  @Test
  public void exceptionOnInjectedMessageProcessor() throws Exception {
    List<Processor> preList = new ArrayList<>();
    List<Processor> postList = new ArrayList<>();

    Flow flow = getFlow("exceptionFlow");
    preList.add(new StringAppendTransformer("(pre)"));
    preList.add(event -> {
      throw new RuntimeException("force exception!");
    });
    postList.add(new StringAppendTransformer("(post)"));
    flow.dynamicPipeline(null).injectBefore(preList).injectAfter(postList).resetAndUpdate();
    InternalMessage result = flowRunner("exceptionFlow").withPayload("source->").run().getMessage();
    assertEquals("source->(pre)(handled)", getPayloadAsString(result));
  }

  @Test
  public void proposeInitialPipelineIdSucceeds() throws Exception {
    String proposedId = "valid-id";
    String pipelineId = getFlow("dynamicFlow").dynamicPipeline(proposedId).resetAndUpdate();

    assertEquals(pipelineId, proposedId);
  }

  @Test
  public void dynamicPipelineCanBeUpdatedAfterFailure() throws Exception {
    String proposedId = "ID";
    Flow flow = getFlow("dynamicFlow");
    try {
      Processor invalidProcessor = new ParseTemplateTransformer();
      flow.dynamicPipeline(proposedId).injectBefore(invalidProcessor).resetAndUpdate();
    } catch (MuleException e) {
      // Expected failure
    }

    flow.dynamicPipeline(proposedId).injectBefore(new StringAppendTransformer("(pre)"))
        .injectAfter(new StringAppendTransformer("(post)"))
        .resetAndUpdate();

    // Message result = client.send("vm://dynamic", "source->", null);
    InternalMessage result = flowRunner("dynamicFlow").withPayload("source->").run().getMessage();
    assertEquals("source->(pre)(static)(post)", getPayloadAsString(result));
  }

  @Test
  public void applyLifecycle() throws Exception {
    StringBuilder expected = new StringBuilder();

    Flow flow = getFlow("dynamicFlow");
    LifecycleMessageProcessor lifecycleMessageProcessor = new LifecycleMessageProcessor();
    String pipelineId = flow.dynamicPipeline(null).injectBefore(lifecycleMessageProcessor).resetAndUpdate();
    Event muleEvent = flowRunner("dynamicFlow").withPayload("source->").run();
    InternalMessage result = muleEvent.getMessage();
    assertEquals("source->(pre)(static)", getPayloadAsString(result));
    assertEquals(expected.append("ISP").toString(), lifecycleMessageProcessor.getSteps());

    muleEvent = flowRunner("dynamicFlow").withPayload("source->").run();
    result = muleEvent.getMessage();
    assertEquals("source->(pre)(static)", getPayloadAsString(result));
    assertEquals(expected.append("P").toString(), lifecycleMessageProcessor.getSteps());

    flow.dynamicPipeline(pipelineId).reset();
    assertEquals(expected.append("TD").toString(), lifecycleMessageProcessor.getSteps());

    muleEvent = flowRunner("dynamicFlow").withPayload("source->").run();
    result = muleEvent.getMessage();
    assertEquals("source->(static)", getPayloadAsString(result));
    assertEquals(expected.toString(), lifecycleMessageProcessor.getSteps());
  }

  @Test
  public void applyAwareInterfaces() throws Exception {
    Flow flow = getFlow("dynamicFlow");
    UberAwareMessageProcessor awareMessageProcessor = new UberAwareMessageProcessor();
    flow.dynamicPipeline(null).injectBefore(awareMessageProcessor).resetAndUpdate();
    final Event muleEvent = flowRunner("dynamicFlow").withPayload("source->").run();
    InternalMessage result = muleEvent.getMessage();
    assertEquals("source->(pre)(static)", getPayloadAsString(result));
    assertNotNull(awareMessageProcessor.getFlowConstruct());
    assertNotNull(awareMessageProcessor.getMuleContext());
  }

  @Test(expected = DynamicPipelineException.class)
  public void invalidNullPipelineId() throws Exception {
    getFlow("dynamicFlow").dynamicPipeline(null).resetAndUpdate();
    getFlow("dynamicFlow").dynamicPipeline(null).reset();
  }

  @Test(expected = DynamicPipelineException.class)
  public void invalidPipelineId() throws Exception {
    String id = getFlow("dynamicFlow").dynamicPipeline(null).resetAndUpdate();
    getFlow("dynamicFlow").dynamicPipeline(id + "x").reset();
  }

  private static Flow getFlow(String flowName) throws MuleException {
    return (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
  }

  public static class Component implements Callable {

    private String pipelineId;
    private int count;

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("dynamicComponentFlow");
      pipelineId =
          flow.dynamicPipeline(pipelineId).injectBefore(new StringAppendTransformer("chain update #" + ++count)).resetAndUpdate();
      return eventContext.getMessage();
    }

  }

  private static class LifecycleMessageProcessor implements Processor, Lifecycle {

    private StringBuffer steps = new StringBuffer();

    @Override
    public void dispose() {
      steps.append("D");
    }

    @Override
    public void initialise() throws InitialisationException {
      steps.append("I");
    }

    @Override
    public Event process(Event event) throws MuleException {
      steps.append("P");
      return Event.builder(event)
          .message(InternalMessage.builder().payload(event.getMessage().getPayload().getValue() + "(pre)").build())
          .build();
    }

    @Override
    public void start() throws MuleException {
      steps.append("S");
    }

    @Override
    public void stop() throws MuleException {
      steps.append("T");
    }

    public String getSteps() {
      return steps.toString();
    }
  }

  private static class UberAwareMessageProcessor implements Processor, MuleContextAware, FlowConstructAware {

    private FlowConstruct flowConstruct;
    private MuleContext muleContext;

    @Override
    public Event process(Event event) throws MuleException {
      return Event.builder(event)
          .message(InternalMessage.builder().payload(event.getMessage().getPayload().getValue() + "(pre)").build())
          .build();
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct) {

      this.flowConstruct = flowConstruct;
    }

    @Override
    public void setMuleContext(MuleContext muleContext) {
      this.muleContext = muleContext;
    }

    public FlowConstruct getFlowConstruct() {
      return flowConstruct;
    }

    public MuleContext getMuleContext() {
      return muleContext;
    }
  }

}
