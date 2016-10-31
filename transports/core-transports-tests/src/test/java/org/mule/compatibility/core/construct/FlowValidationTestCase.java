/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.construct;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstructInvalidException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.strategy.factory.AsynchronousProcessingStrategyFactory;
import org.mule.runtime.core.api.strategy.factory.SynchronousProcessingStrategyFactory.SynchronousProcessingStrategy;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
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
  public MuleContext mockMuleContext = mockContextWithServices();
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  public InboundEndpoint inboundEndpoint;
  @Mock
  public OnErrorPropagateHandler onErrorPropagateHandler;
  @Mock
  public AbstractRedeliveryPolicy mockRedeliveryPolicy;
  private Flow flow;

  @Before
  public void setUp() throws RegistrationException {
    when(mockMuleContext.getConfiguration().getDefaultProcessingStrategyFactory()).thenReturn(null);
    this.flow = new Flow(FLOW_NAME, mockMuleContext);
  }

  @Test(expected = FlowConstructInvalidException.class)
  public void testProcessingStrategyCantBeAsyncWithRedelivery() throws Exception {
    configureFlowForRedelivery();
    flow.setProcessingStrategyFactory(new AsynchronousProcessingStrategyFactory());
    flow.validateConstruct();
  }

  @Test
  public void testChangeDefaultProcessingStrategyWithRedelivery() throws Exception {
    configureFlowForRedelivery();
    flow.validateConstruct();
    assertThat(flow.getProcessingStrategy(), instanceOf(SynchronousProcessingStrategy.class));
  }

  private void configureFlowForRedelivery() {
    when(inboundEndpoint.getTransactionConfig().isConfigured()).thenReturn(false);
    when(inboundEndpoint.getExchangePattern().hasResponse()).thenReturn(false);
    flow.setMessageProcessors(Arrays.asList(new IdempotentRedeliveryPolicy()));
    flow.setMessageSource(inboundEndpoint);
    // flow.setMessageSource(Mockito.mock(MessageSource.class));
  }

}
