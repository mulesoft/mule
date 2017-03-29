/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory.SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
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
    this.flow = builder(FLOW_NAME, mockMuleContext).build();
  }

  @Test
  public void testChangeDefaultProcessingStrategyWithRedelivery() throws Exception {
    configureFlowForRedelivery();
    flow.initialise();
    assertThat(flow.getProcessingStrategy(), equalTo(SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE));
  }

  private void configureFlowForRedelivery() {
    final IdempotentRedeliveryPolicy idempotentRedeliveryPolicy = new IdempotentRedeliveryPolicy();
    idempotentRedeliveryPolicy.setUseSecureHash(true);
    flow.setMessageProcessors(asList(idempotentRedeliveryPolicy));
    flow.setMessageSource(mock(MessageSource.class));
  }

}
