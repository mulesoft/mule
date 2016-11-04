/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstructInvalidException;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.source.NonBlockingMessageSource;
import org.mule.runtime.core.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory.SynchronousProcessingStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FlowValidationTestCase extends AbstractMuleTestCase {

  public static final String FLOW_NAME = "flowName";

  public MuleContext mockMuleContext = mockContextWithServices();
  @Mock
  public OnErrorPropagateHandler onErrorPropagateHandler;
  @Mock
  public AbstractRedeliveryPolicy mockRedeliveryPolicy;
  private Flow flow;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void setUp() throws MuleException {
    when(mockMuleContext.getConfiguration().getDefaultProcessingStrategyFactory()).thenReturn(null);
    this.flow = new Flow(FLOW_NAME, mockMuleContext);
  }

  @Test
  public void testProcessingStrategyCantBeAsyncWithRedelivery() throws Exception {
    configureFlowForRedelivery();
    flow.setProcessingStrategyFactory(new AsynchronousProcessingStrategyFactory());

    expected.expectCause(hasCause(instanceOf(FlowConstructInvalidException.class)));
    expected.expectMessage("One of the message sources configured on this Flow is not "
        + "compatible with an asynchronous processing strategy.  Either "
        + "because it is request-response, has a transaction defined, or " + "messaging redelivered is configured.");
    flow.initialise();
  }

  @Test
  public void testProcessingStrategyNonBlockingSupported() throws Exception {
    flow.setProcessingStrategyFactory(new NonBlockingProcessingStrategyFactory());
    flow.setMessageSource((NonBlockingMessageSource) listener -> {
    });
    flow.initialise();
  }

  @Test
  public void testProcessingStrategyNonBlockingNotSupported() throws Exception {
    flow.setProcessingStrategyFactory(new NonBlockingProcessingStrategyFactory());
    flow.setMessageSource(listener -> {
    });

    expected.expectCause(hasCause(instanceOf(FlowConstructInvalidException.class)));
    expected.expectMessage(allOf(containsString("The non-blocking processing strategy"),
                                 containsString("currently only supports non-blocking messages sources (source is")));
    flow.initialise();
  }

  @Test
  public void testChangeDefaultProcessingStrategyWithRedelivery() throws Exception {
    configureFlowForRedelivery();
    flow.initialise();
    assertThat(flow.getProcessingStrategy(), instanceOf(SynchronousProcessingStrategy.class));
  }

  private void configureFlowForRedelivery() {
    final IdempotentRedeliveryPolicy idempotentRedeliveryPolicy = new IdempotentRedeliveryPolicy();
    idempotentRedeliveryPolicy.setUseSecureHash(true);
    flow.setMessageProcessors(asList(idempotentRedeliveryPolicy));
    flow.setMessageSource(mock(MessageSource.class));
  }

}
