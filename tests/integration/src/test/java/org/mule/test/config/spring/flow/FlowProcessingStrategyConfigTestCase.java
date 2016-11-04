/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.flow;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
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
    assertThat(getFlowProcessingStrategy("defaultFlow"), instanceOf(DefaultFlowProcessingStrategy.class));
  }

  @Test
  public void testSynchronous() throws Exception {
    assertThat(getFlowProcessingStrategy("synchronousFlow"), instanceOf(SynchronousProcessingStrategy.class));
  }

  @Test
  public void testAsynchronous() throws Exception {
    assertThat(getFlowProcessingStrategy("asynchronousFlow"), instanceOf(AsynchronousProcessingStrategy.class));
  }

  @Test
  public void testNonBlocking() throws Exception {
    assertThat(getFlowProcessingStrategy("nonBlockingFlow"), instanceOf(NonBlockingProcessingStrategy.class));
  }

  @Test
  public void testCustom() throws Exception {
    ProcessingStrategy processingStrategy = getFlowProcessingStrategy("customProcessingStrategyFlow");
    assertThat(processingStrategy, instanceOf(CustomProcessingStrategy.class));

    assertThat(((CustomProcessingStrategy) processingStrategy).foo, is("bar"));
  }

  @Test
  public void testDefaultAsync() throws Exception {
    assertThat(getFlowProcessingStrategy("defaultAsync"), instanceOf(AsynchronousProcessingStrategy.class));
  }

  @Test
  public void testAsynchronousAsync() throws Exception {
    assertThat(getFlowProcessingStrategy("asynchronousAsync"), instanceOf(AsynchronousProcessingStrategy.class));
  }

  @Test
  public void testCustomAsync() throws Exception {
    ProcessingStrategy processingStrategy = getAsyncProcessingStrategy("customProcessingStrategyAsync");
    assertThat(processingStrategy, instanceOf(CustomProcessingStrategy.class));

    assertThat(((CustomProcessingStrategy) processingStrategy).foo, is("bar"));
  }

  private ProcessingStrategy getFlowProcessingStrategy(String flowName) throws Exception {
    Flow flow = (Flow) getFlowConstruct(flowName);
    return flow.getProcessingStrategy();
  }

  private ProcessingStrategy getAsyncProcessingStrategy(String flowName) throws Exception {
    Flow flow = (Flow) getFlowConstruct(flowName);
    Processor processor = flow.getMessageProcessors().get(0);
    assertThat(processor, instanceOf(AsyncDelegateMessageProcessor.class));
    return ((AsyncDelegateMessageProcessor) processor).getProcessingStrategy();
  }

  public static class CustomProcessingStrategy implements ProcessingStrategy, ProcessingStrategyFactory {

    String foo;

    @Override
    public void configureProcessors(List<Processor> processors, MessageProcessorChainBuilder chainBuilder) {
      // Nothing to do
    }

    @Override
    public ProcessingStrategy create(MuleContext muleContext) {
      return this;
    }

    public void setFoo(String foo) {
      this.foo = foo;

    }
  }

}
