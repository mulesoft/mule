/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.flow;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategyFactory.AsynchronousProcessingStrategy;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategyFactory.NonBlockingProcessingStrategy;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory.SynchronousProcessingStrategy;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import org.junit.Test;

public class FlowProcessingStrategyConfigTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/spring/flow/flow-processing-strategies.xml";
  }

  @Test
  public void testDefault() throws Exception {
    assertEquals(DefaultFlowProcessingStrategy.class, getFlowProcessingStrategy("defaultFlow").getClass());
  }

  @Test
  public void testSynchronous() throws Exception {
    assertEquals(SynchronousProcessingStrategy.class, getFlowProcessingStrategy("synchronousFlow").getClass());
  }

  @Test
  public void testAsynchronous() throws Exception {
    assertEquals(AsynchronousProcessingStrategy.class, getFlowProcessingStrategy("asynchronousFlow").getClass());
  }

  @Test
  public void testNonBlocking() throws Exception {
    assertEquals(NonBlockingProcessingStrategy.class, getFlowProcessingStrategy("nonBlockingFlow").getClass());
  }

  @Test
  public void testCustomAsynchronous() throws Exception {
    ProcessingStrategy processingStrategy = getFlowProcessingStrategy("customAsynchronousFlow");

    assertEquals(AsynchronousProcessingStrategy.class, processingStrategy.getClass());
  }

  @Test
  public void testCustomNonBlocking() throws Exception {
    ProcessingStrategy processingStrategy = getFlowProcessingStrategy("customNonBlockingFlow");

    assertEquals(NonBlockingProcessingStrategy.class, processingStrategy.getClass());
  }

  @Test
  public void testCustom() throws Exception {
    ProcessingStrategy processingStrategy = getFlowProcessingStrategy("customProcessingStrategyFlow");
    assertEquals(CustomProcessingStrategy.class, processingStrategy.getClass());

    assertEquals("bar", (((CustomProcessingStrategy) processingStrategy).foo));
  }

  @Test
  public void testDefaultAsync() throws Exception {
    assertEquals(AsynchronousProcessingStrategy.class, getAsyncProcessingStrategy("defaultAsync").getClass());
  }

  @Test
  public void testAsynchronousAsync() throws Exception {
    assertEquals(AsynchronousProcessingStrategy.class, getAsyncProcessingStrategy("asynchronousAsync").getClass());
  }

  @Test
  public void testCustomAsynchronousAsync() throws Exception {
    ProcessingStrategy processingStrategy = getAsyncProcessingStrategy("customAsynchronousAsync");

    assertEquals(AsynchronousProcessingStrategy.class, processingStrategy.getClass());
  }

  @Test
  public void testCustomAsync() throws Exception {
    ProcessingStrategy processingStrategy = getAsyncProcessingStrategy("customProcessingStrategyAsync");
    assertEquals(CustomProcessingStrategy.class, processingStrategy.getClass());

    assertEquals("bar", (((CustomProcessingStrategy) processingStrategy).foo));
  }

  private ProcessingStrategy getFlowProcessingStrategy(String flowName) throws Exception {
    Flow flow = (Flow) getFlowConstruct(flowName);
    return flow.getProcessingStrategy();
  }

  private ProcessingStrategy getAsyncProcessingStrategy(String flowName) throws Exception {
    Flow flow = (Flow) getFlowConstruct(flowName);
    Processor processor = flow.getMessageProcessors().get(0);
    assertEquals(AsyncDelegateMessageProcessor.class, processor.getClass());
    return ((AsyncDelegateMessageProcessor) processor).getProcessingStrategy();
  }

  public static class CustomProcessingStrategy implements ProcessingStrategy, ProcessingStrategyFactory {

    String foo;

    @Override
    public void configureProcessors(List<Processor> processors, SchedulerService schedulerService,
                                    MessageProcessorChainBuilder chainBuilder, MuleContext muleContext) {
      // Nothing to do
    }

    @Override
    public ProcessingStrategy create() {
      return this;
    }

    public void setFoo(String foo) {
      this.foo = foo;

    }
  }

}
