/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;

//TODO MULE-10927 - create a common class between CompositeOperationPolicyTestCase and CompositeSourcePolicyTestCase
public class CompositeSourcePolicyTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private CompositeSourcePolicy compositeSourcePolicy;

  private final FlowConstruct mockFlowConstruct = mock(FlowConstruct.class, RETURNS_DEEP_STUBS);
  private final Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer =
      of(mock(SourcePolicyParametersTransformer.class));
  private final MessageSourceResponseParametersProcessor sourceParametersProcessor =
      mock(MessageSourceResponseParametersProcessor.class, RETURNS_DEEP_STUBS);
  private final Policy firstPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  private final Policy secondPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  private CoreEvent initialEvent;
  private CoreEvent modifiedEvent;
  private CoreEvent firstPolicyResultEvent;
  private CoreEvent secondPolicyResultEvent;
  private final Processor flowExecutionProcessor = mock(Processor.class);
  private final CoreEvent nextProcessResultEvent = mock(CoreEvent.class);
  private final SourcePolicyProcessorFactory sourcePolicyProcessorFactory =
      mock(SourcePolicyProcessorFactory.class, RETURNS_DEEP_STUBS);

  private CoreEvent flowActualResultEvent;
  private CoreEvent firstPolicyActualResultEvent;
  private CoreEvent secondPolicyActualResultEvent;

  @Before
  public void setUp() throws Exception {
    initialEvent = createTestEvent();
    modifiedEvent = createTestEvent();
    firstPolicyResultEvent = createTestEvent();
    secondPolicyResultEvent = createTestEvent();

    when(nextProcessResultEvent.getMessage()).thenReturn(mock(Message.class));
    when(flowExecutionProcessor.apply(any())).thenAnswer(invocation -> {
      return Flux.from((Publisher<CoreEvent>) invocation.getArgument(0))
          .doOnNext(ev -> flowActualResultEvent = ev)
          .doOnNext(event -> ((BaseEventContext) event.getContext()).success(event));
    });
    when(firstPolicy.getPolicyChain().apply(any())).thenReturn(just(firstPolicyResultEvent));
    when(secondPolicy.getPolicyChain().apply(any())).thenReturn(just(secondPolicyResultEvent));

    when(sourcePolicyProcessorFactory.createSourcePolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      Processor firstPolicySourcePolicyProcessor = mock(Processor.class);
      when(firstPolicySourcePolicyProcessor.apply(any()))
          .thenAnswer(policyProcessorInvocation -> {
            return Flux.from((Publisher<CoreEvent>) policyProcessorInvocation.getArguments()[0])
                .doOnNext(ev -> firstPolicyActualResultEvent = ev)
                .map(e -> CoreEvent.builder(e).message(modifiedEvent.getMessage()).build())
                .transform((ReactiveProcessor) policyFactoryInvocation.getArguments()[1])
                .map(e -> CoreEvent.builder(e).message(firstPolicyResultEvent.getMessage()).build());
          });
      return firstPolicySourcePolicyProcessor;
    });
    when(sourcePolicyProcessorFactory.createSourcePolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      Processor secondPolicySourcePolicyProcessor = mock(Processor.class);
      when(secondPolicySourcePolicyProcessor.apply(any()))
          .thenAnswer(policyProcessorInvocation -> {
            return Flux.from((Publisher<CoreEvent>) policyProcessorInvocation.getArguments()[0])
                .doOnNext(ev -> secondPolicyActualResultEvent = ev)
                .map(e -> CoreEvent.builder(e).message(modifiedEvent.getMessage()).build())
                .transform((ReactiveProcessor) policyFactoryInvocation.getArguments()[1])
                .map(e -> CoreEvent.builder(e).message(secondPolicyResultEvent.getMessage()).build());
          });
      return secondPolicySourcePolicyProcessor;
    });
  }

  @Test
  public void singlePolicy() throws Exception {
    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy), flowExecutionProcessor,
                                                      sourcePolicyParametersTransformer, sourcePolicyProcessorFactory);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> sourcePolicyResult =
        from(compositeSourcePolicy.process(initialEvent, sourceParametersProcessor)).block();
    assertThat(sourcePolicyResult.isRight(), is(true));
    assertThat(sourcePolicyResult.getRight().getResult().getMessage(), is(firstPolicyResultEvent.getMessage()));
    verify(flowExecutionProcessor, atLeastOnce()).apply(any());
    assertThat(flowActualResultEvent.getMessage(),
               equalTo(modifiedEvent.getMessage()));

    verify(sourcePolicyProcessorFactory, atLeastOnce()).createSourcePolicy(same(firstPolicy), any());
    assertThat(firstPolicyActualResultEvent.getMessage(),
               equalTo(initialEvent.getMessage()));
  }

  @Test
  public void compositePolicy() throws Exception {
    compositeSourcePolicy =
        new CompositeSourcePolicy(asList(firstPolicy, secondPolicy), flowExecutionProcessor, sourcePolicyParametersTransformer,
                                  sourcePolicyProcessorFactory);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> sourcePolicyResult =
        from(compositeSourcePolicy.process(initialEvent, sourceParametersProcessor)).block();
    assertThat(sourcePolicyResult.isRight(), is(true));
    assertThat(sourcePolicyResult.getRight().getResult().getMessage(), is(firstPolicyResultEvent.getMessage()));
    verify(flowExecutionProcessor, atLeastOnce()).apply(any());
    assertThat(flowActualResultEvent.getMessage(),
               equalTo(modifiedEvent.getMessage()));
    verify(sourcePolicyProcessorFactory, atLeastOnce()).createSourcePolicy(same(firstPolicy), any());
    verify(sourcePolicyProcessorFactory, atLeastOnce()).createSourcePolicy(same(secondPolicy), any());

    assertThat(firstPolicyActualResultEvent.getMessage(),
               equalTo(initialEvent.getMessage()));

    assertThat(secondPolicyActualResultEvent.getMessage(),
               equalTo(modifiedEvent.getMessage()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyPolicyList() throws Exception {
    compositeSourcePolicy = new CompositeSourcePolicy(emptyList(), flowExecutionProcessor,
                                                      sourcePolicyParametersTransformer, sourcePolicyProcessorFactory);
  }

  @Test
  public void policyExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(sourcePolicyProcessorFactory.createSourcePolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      Processor secondPolicySourcePolicyProcessor = mock(Processor.class);
      when(secondPolicySourcePolicyProcessor.apply(any(Publisher.class)))
          .thenAnswer(inv -> Flux.from((Publisher<CoreEvent>) inv.getArgument(0))
              .flatMap(event -> error(new MessagingException(event, policyException))));
      return secondPolicySourcePolicyProcessor;
    });

    compositeSourcePolicy =
        new CompositeSourcePolicy(asList(firstPolicy, secondPolicy), flowExecutionProcessor, sourcePolicyParametersTransformer,
                                  sourcePolicyProcessorFactory);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> sourcePolicyResult =
        from(compositeSourcePolicy.process(initialEvent, sourceParametersProcessor)).block();
    assertThat(sourcePolicyResult.isLeft(), is(true));
    assertThat(sourcePolicyResult.getLeft().getMessagingException().getCause(), is(policyException));
  }

  @Test
  public void nextProcessorExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    reset(flowExecutionProcessor);
    when(flowExecutionProcessor.apply(any()))
        .thenAnswer(invocation -> Flux.from((Publisher<CoreEvent>) invocation.getArgument(0))
            .flatMap(event -> error(new MessagingException(event, policyException))));
    compositeSourcePolicy =
        new CompositeSourcePolicy(asList(firstPolicy, secondPolicy), flowExecutionProcessor, sourcePolicyParametersTransformer,
                                  sourcePolicyProcessorFactory);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> sourcePolicyResult =
        from(compositeSourcePolicy.process(initialEvent, sourceParametersProcessor)).block();
    assertThat(sourcePolicyResult.isLeft(), is(true));
    assertThat(sourcePolicyResult.getLeft().getMessagingException().getCause(), is(policyException));
  }

  @Test
  public void reactorPipelinesReused() {
    InvocationsRecordingCompositeSourcePolicy.reset();
    final InvocationsRecordingCompositeSourcePolicy sourcePolicy =
        new InvocationsRecordingCompositeSourcePolicy(asList(firstPolicy), flowExecutionProcessor,
                                                      sourcePolicyParametersTransformer,
                                                      sourcePolicyProcessorFactory);

    assertThat(sourcePolicy.getNextOperationCount(), is(getRuntime().availableProcessors()));
    assertThat(sourcePolicy.getPolicyCount(), is(getRuntime().availableProcessors()));

    for (int i = 0; i < getRuntime().availableProcessors() * 2; ++i) {
      from(sourcePolicy.process(initialEvent, sourceParametersProcessor)).block();
    }

    assertThat(sourcePolicy.getNextOperationCount(), is(getRuntime().availableProcessors()));
    assertThat(sourcePolicy.getPolicyCount(), is(getRuntime().availableProcessors()));
  }

  private CoreEvent createTestEvent() {
    return CoreEvent.builder(create(mockFlowConstruct, fromSingleComponent("http")))
        .message(Message.builder().nullValue().build()).build();
  }

  public static final class InvocationsRecordingCompositeSourcePolicy extends CompositeSourcePolicy {

    private static final AtomicInteger nextOperation = new AtomicInteger();
    private static final AtomicInteger policy = new AtomicInteger();

    public InvocationsRecordingCompositeSourcePolicy(List<Policy> parameterizedPolicies, ReactiveProcessor flowExecutionProcessor,
                                                     Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                                                     SourcePolicyProcessorFactory sourcePolicyProcessorFactory) {
      super(parameterizedPolicies, flowExecutionProcessor, sourcePolicyParametersTransformer, sourcePolicyProcessorFactory);
    }

    public static void reset() {
      nextOperation.set(0);
      policy.set(0);
    }

    @Override
    protected Publisher<CoreEvent> applyNextOperation(Publisher<CoreEvent> eventPub) {
      nextOperation.incrementAndGet();
      return super.applyNextOperation(eventPub);
    }

    @Override
    protected Publisher<CoreEvent> applyPolicy(Policy policy, ReactiveProcessor nextProcessor, Publisher<CoreEvent> eventPub) {
      InvocationsRecordingCompositeSourcePolicy.policy.incrementAndGet();
      return super.applyPolicy(policy, nextProcessor, eventPub);
    }

    public int getNextOperationCount() {
      return nextOperation.get();
    }

    public int getPolicyCount() {
      return policy.get();
    }
  }
}
