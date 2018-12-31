/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.reactivestreams.Publisher;

import java.util.function.Function;

import reactor.core.publisher.Flux;


public abstract class AbstractCompositePolicyTestCase extends AbstractMuleContextTestCase {

  private final FlowConstruct mockFlowConstruct = mock(FlowConstruct.class, RETURNS_DEEP_STUBS);

  protected CoreEvent firstPolicyResultEvent;
  protected CoreEvent secondPolicyResultEvent;
  protected final Policy firstPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  protected final Policy secondPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);

  private CoreEvent firstPolicyActualResultEvent;
  private CoreEvent secondPolicyActualResultEvent;

  @Before
  public void commonBefore() {
    firstPolicyResultEvent = createTestEvent();
    secondPolicyResultEvent = createTestEvent();

    when(firstPolicy.getPolicyChain().apply(any())).thenReturn(just(firstPolicyResultEvent));
    when(secondPolicy.getPolicyChain().apply(any())).thenReturn(just(secondPolicyResultEvent));
  }

  protected CoreEvent createTestEvent() {
    return CoreEvent.builder(create(mockFlowConstruct, fromSingleComponent("http")))
        .message(Message.builder().nullValue().build()).build();
  }

  protected Processor firstPolicyProcessor(InvocationOnMock policyFactoryInvocation,
                                           Function<CoreEvent, CoreEvent> beforeMapper,
                                           Function<CoreEvent, CoreEvent> afterMapper) {
    Processor firstPolicyProcessor = mock(Processor.class);
    when(firstPolicyProcessor.apply(any()))
        .thenAnswer(policyProcessorInvocation -> {
          return Flux.from((Publisher<CoreEvent>) policyProcessorInvocation.getArguments()[0])
              .doOnNext(ev -> firstPolicyActualResultEvent = ev)
              .map(beforeMapper)
              .transform((ReactiveProcessor) policyFactoryInvocation.getArguments()[1])
              .map(afterMapper);
        });
    return firstPolicyProcessor;
  }

  protected Processor secondPolicyProcessor(InvocationOnMock policyFactoryInvocation,
                                            Function<CoreEvent, CoreEvent> beforeMapper,
                                            Function<CoreEvent, CoreEvent> afterMapper) {
    Processor secondPolicyProcessor = mock(Processor.class);
    when(secondPolicyProcessor.apply(any()))
        .thenAnswer(policyProcessorInvocation -> {
          return Flux.from((Publisher<CoreEvent>) policyProcessorInvocation.getArguments()[0])
              .doOnNext(ev -> secondPolicyActualResultEvent = ev)
              .map(beforeMapper)
              .transform((ReactiveProcessor) policyFactoryInvocation.getArguments()[1])
              .map(afterMapper);
        });
    return secondPolicyProcessor;
  }

  public CoreEvent getFirstPolicyActualResultEvent() {
    return firstPolicyActualResultEvent;
  }

  public CoreEvent getSecondPolicyActualResultEvent() {
    return secondPolicyActualResultEvent;
  }

}
