/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProcessingStrategyFactoryTest {

  @Mock
  private MuleContext context;
  @Mock
  private FlowConstruct flow;
  @Mock
  private ReactiveProcessor pipeline;
  @Mock
  private ProcessingStrategy delegate;
  @Mock
  private Injector injector;
  @InjectMocks
  private TransactionAwareStreamEmitterProcessingStrategyFactory taseFactory;
  @InjectMocks
  private DirectStreamPerThreadProcessingStrategyFactory dsptFactory;

  @BeforeEach
  public void setUp() throws Exception {}

  @Test
  public void directStreamPerThreadFactory() {
    final Sink result = dsptFactory.create(context, "foo").createSink(flow, pipeline);

    assertThat(result, is(notNullValue()));
  }

  @Test
  public void transactionAwareDirectStreamFactory() {
    when(context.getInjector()).thenReturn(injector);
    final Sink result = taseFactory.create(context, "foo").createSink(flow, pipeline);

    assertThat(result, is(notNullValue()));
  }
}
