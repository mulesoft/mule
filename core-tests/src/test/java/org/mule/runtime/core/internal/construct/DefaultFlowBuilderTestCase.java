/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.construct;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.IsNot.not;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.Flow.Builder;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class DefaultFlowBuilderTestCase extends AbstractMuleTestCase {

  public static final String FLOW_NAME = "flowName";

  private MuleContext muleContext = mockContextWithServices();
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
    Flow flow = flowBuilder.build();
    assertThat(flow.getName(), equalTo(FLOW_NAME));
    assertThat(flow.getMuleContext(), is(muleContext));
    assertThat(flow.getProcessors(), is(empty()));
    assertThat(flow.getSource(), is(nullValue()));
    assertThat(flow.getExceptionListener(), not(sameInstance(muleContext.getDefaultErrorHandler(Optional.empty()))));
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
    FlowExceptionHandler exceptionListener = mock(FlowExceptionHandler.class);

    Flow flow = flowBuilder.processors(messageProcessors).source(messageSource)
        .processingStrategyFactory(processingStrategyFactory).messagingExceptionHandler(exceptionListener).build();

    assertThat(flow.getName(), equalTo(FLOW_NAME));
    assertThat(flow.getMuleContext(), is(muleContext));
    assertThat(flow.getProcessors(), contains(processor1, processor2));
    assertThat(flow.getSource(), is(messageSource));
    assertThat(flow.getExceptionListener(), is(exceptionListener));
    assertThat(flow.getProcessingStrategy(), sameInstance(processingStrategy));
  }

  @Test
  public void cannotBuildFlowTwice() throws Exception {
    flowBuilder.build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.build();
  }

  @Test
  public void cannotChangeMessageSourceAfterFlowBuilt() throws Exception {
    flowBuilder.build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.source(mock(MessageSource.class));
  }

  @Test
  public void cannotChangeMessageProcessorsListAfterFlowBuilt() throws Exception {
    flowBuilder.build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.processors(asList(mock(Processor.class)));
  }

  @Test
  public void cannotChangeMessageProcessorAfterFlowBuilt() throws Exception {
    flowBuilder.build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.processors(mock(Processor.class));
  }

  @Test
  public void cannotChangeExceptionListenerAfterFlowBuilt() throws Exception {
    flowBuilder.build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.messagingExceptionHandler(mock(FlowExceptionHandler.class));
  }

  @Test
  public void cannotChangeProcessingStrategyFactoryAfterFlowBuilt() throws Exception {
    flowBuilder.build();
    expectedException.expect(IllegalStateException.class);
    flowBuilder.processingStrategyFactory(mock(ProcessingStrategyFactory.class));
  }
}
