/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.construct;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.Flow.Builder;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class DefaultFlowBuilderTestCase extends AbstractMuleTestCase {

  public static final String FLOW_NAME = "flowName";

  private MuleContext muleContext = mock(MuleContext.class);
  private Builder flowBuilder = new DefaultFlowBuilder(FLOW_NAME, muleContext);
  private ProcessingStrategyFactory defaultProcessingStrategyFactory = mock(ProcessingStrategyFactory.class);
  private ProcessingStrategy processingStrategy = mock(ProcessingStrategy.class);

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void setUp() throws Exception {
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    when(muleConfiguration.getDefaultProcessingStrategyFactory()).thenReturn(defaultProcessingStrategyFactory);
    when(defaultProcessingStrategyFactory.create(any(), any())).thenReturn(processingStrategy);
  }

  @Test
  public void buildsSimpleFlow() throws Exception {
    Flow flow = flowBuilder.messageProcessors(emptyList()).build();

    assertThat(flow.getName(), equalTo(FLOW_NAME));
    assertThat(flow.getMuleContext(), is(muleContext));
    assertThat(flow.getMessageProcessors(), is(empty()));
    assertThat(flow.getMessageSource(), is(nullValue()));
    assertThat(flow.getExceptionListener(), is(nullValue()));
    assertThat(flow.getProcessingStrategy(), sameInstance(processingStrategy));
  }

  @Test
  public void buildsFullFlow() throws Exception {
    Processor processor1 = mock(Processor.class);
    Processor processor2 = mock(Processor.class);
    List<Processor> messageProcessors = new ArrayList<>();
    messageProcessors.add(processor1);
    messageProcessors.add(processor2);

    MessageSource messageSource = mock(MessageSource.class);
    ProcessingStrategyFactory processingStrategyFactory = mock(ProcessingStrategyFactory.class);
    ProcessingStrategy processingStrategy = mock(ProcessingStrategy.class);
    when(processingStrategyFactory.create(any(), any())).thenReturn(processingStrategy);
    MessagingExceptionHandler exceptionListener = mock(MessagingExceptionHandler.class);

    Flow flow = flowBuilder.messageProcessors(messageProcessors).messageSource(messageSource)
        .processingStrategyFactory(processingStrategyFactory).messagingExceptionHandler(exceptionListener).build();

    assertThat(flow.getName(), equalTo(FLOW_NAME));
    assertThat(flow.getMuleContext(), is(muleContext));
    assertThat(flow.getMessageProcessors(), contains(processor1, processor2));
    assertThat(flow.getMessageSource(), is(messageSource));
    assertThat(flow.getExceptionListener(), is(exceptionListener));
    assertThat(flow.getProcessingStrategy(), sameInstance(processingStrategy));
  }

  @Test
  public void cannotBuildFlowTwice() throws Exception {
    flowBuilder.messageProcessors(emptyList()).build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.messageProcessors(emptyList()).build();
  }

  @Test
  public void cannotChangeMessageSourceAfterFlowBuilt() throws Exception {
    flowBuilder.messageProcessors(emptyList()).build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.messageProcessors(emptyList()).messageSource(mock(MessageSource.class));
  }

  @Test
  public void cannotChangeMessageProcessorsAfterFlowBuilt() throws Exception {
    flowBuilder.messageProcessors(emptyList()).build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.messageProcessors(new ArrayList<>());
  }

  @Test
  public void cannotChangeExceptionListenerAfterFlowBuilt() throws Exception {
    flowBuilder.messageProcessors(emptyList()).build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.messagingExceptionHandler(mock(MessagingExceptionHandler.class));
  }

  @Test
  public void cannotChangeProcessingStrategyFactoryAfterFlowBuilt() throws Exception {
    flowBuilder.messageProcessors(emptyList()).build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.processingStrategyFactory(mock(ProcessingStrategyFactory.class));
  }
}
