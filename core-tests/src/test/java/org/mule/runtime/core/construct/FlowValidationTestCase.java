/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstructInvalidException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.source.NonBlockingMessageSource;
import org.mule.runtime.core.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FlowValidationTestCase extends AbstractMuleTestCase {

  public static final String FLOW_NAME = "flowName";
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  public MuleContext mockMuleContext;
  @Mock
  public OnErrorPropagateHandler onErrorPropagateHandler;
  @Mock
  public AbstractRedeliveryPolicy mockRedeliveryPolicy;
  private Flow flow;

  @Before
  public void setUp() {
    when(mockMuleContext.getConfiguration().getDefaultProcessingStrategy()).thenReturn(null);
    this.flow = new Flow(FLOW_NAME, mockMuleContext);
  }

  @Test(expected = FlowConstructInvalidException.class)
  public void testProcessingStrategyCantBeAsyncWithRedelivery() throws Exception {
    configureFlowForRedelivery();
    flow.setProcessingStrategy(new AsynchronousProcessingStrategy());
    flow.validateConstruct();
  }

  @Test
  public void testProcessingStrategyNonBlockingSupported() throws Exception {
    flow.setProcessingStrategy(new NonBlockingProcessingStrategy());
    flow.setMessageSource((NonBlockingMessageSource) listener -> {
    });
    flow.validateConstruct();
  }

  @Test(expected = FlowConstructInvalidException.class)
  public void testProcessingStrategyNonBlockingNotSupported() throws Exception {
    flow.setProcessingStrategy(new NonBlockingProcessingStrategy());
    flow.setMessageSource(listener -> {
    });
    flow.validateConstruct();
  }

  @Test
  public void testChangeDefaultProcessingStrategyWithRedelivery() throws Exception {
    configureFlowForRedelivery();
    flow.validateConstruct();
    assertThat(flow.getProcessingStrategy(), instanceOf(SynchronousProcessingStrategy.class));
  }

  private void configureFlowForRedelivery() {
    flow.setMessageProcessors(Arrays.asList(new IdempotentRedeliveryPolicy()));
    flow.setMessageSource(mock(MessageSource.class));
  }

}
