/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

public class CompositeSourcePolicyTestCase extends AbstractCompositePolicyTestCase {

  private CompositeSourcePolicy compositeSourcePolicy;

  private final Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer =
      of(mock(SourcePolicyParametersTransformer.class));
  private final MessageSourceResponseParametersProcessor sourceParametersProcessor =
      mock(MessageSourceResponseParametersProcessor.class, RETURNS_DEEP_STUBS);

  private CoreEvent modifiedEvent;
  private final Processor flowExecutionProcessor = mock(Processor.class);
  private final SourcePolicyProcessorFactory sourcePolicyProcessorFactory =
      mock(SourcePolicyProcessorFactory.class, RETURNS_DEEP_STUBS);

  private final ArgumentCaptor<Publisher> publisherArgumentCaptor = forClass(Publisher.class);
  private CoreEvent firstPolicyActualResultEvent;
  private CoreEvent secondPolicyActualResultEvent;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    modifiedEvent = createTestEvent();

    when(flowExecutionProcessor.apply(any())).thenAnswer(invocation -> {
      Mono<CoreEvent> mono = from(invocation.getArgument(0));
      return mono.doOnNext(event -> ((BaseEventContext) event.getContext()).success(event));
    });

    when(sourcePolicyProcessorFactory.createSourcePolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(firstPolicyProcessor.apply(any()))
          .thenAnswer(firstPolicyProcessorAnswer(policyFactoryInvocation, ev -> firstPolicyActualResultEvent = ev));
      return firstPolicyProcessor;
    });
    when(sourcePolicyProcessorFactory.createSourcePolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(secondPolicyProcessor.apply(any()))
          .thenAnswer(secondPolicyProcessorAnswer(policyFactoryInvocation, ev -> secondPolicyActualResultEvent = ev));
      return secondPolicyProcessor;
    });
  }

  private Answer<?> firstPolicyProcessorAnswer(InvocationOnMock policyFactoryInvocation,
                                               Consumer<CoreEvent> eventInputCallback) {
    return policyProcessorInvocation -> from((Publisher<CoreEvent>) policyProcessorInvocation.getArguments()[0])
        .doOnNext(eventInputCallback)
        .map(e -> CoreEvent.builder(e).message(modifiedEvent.getMessage()).build())
        .transform((ReactiveProcessor) policyFactoryInvocation.getArguments()[1])
        .map(e -> CoreEvent.builder(e).message(firstPolicyResultEvent.getMessage()).build());
  }

  private Answer<?> secondPolicyProcessorAnswer(InvocationOnMock policyFactoryInvocation,
                                                Consumer<CoreEvent> eventInputCallback) {
    return policyProcessorInvocation -> from((Publisher<CoreEvent>) policyProcessorInvocation.getArguments()[0])
        .doOnNext(eventInputCallback)
        .map(e -> CoreEvent.builder(e).message(modifiedEvent.getMessage()).build())
        .transform((ReactiveProcessor) policyFactoryInvocation.getArguments()[1])
        .map(e -> CoreEvent.builder(e).message(secondPolicyResultEvent.getMessage()).build());
  }

  @Test
  public void singlePolicy() throws Exception {
    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy),
                                                      sourcePolicyParametersTransformer, sourcePolicyProcessorFactory);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> sourcePolicyResult =
        from(compositeSourcePolicy.process(initialEvent, flowExecutionProcessor, sourceParametersProcessor)).block();
    assertThat(sourcePolicyResult.isRight(), is(true));
    assertThat(sourcePolicyResult.getRight().getResult().getMessage(), is(firstPolicyResultEvent.getMessage()));
    verify(flowExecutionProcessor).apply(publisherArgumentCaptor.capture());
    assertThat(((CoreEvent) from(publisherArgumentCaptor.getValue()).block()).getMessage(),
               equalTo(modifiedEvent.getMessage()));

    verify(sourcePolicyProcessorFactory).createSourcePolicy(same(firstPolicy), any());
    verify(firstPolicyProcessor).apply(publisherArgumentCaptor.capture());
    assertThat(firstPolicyActualResultEvent.getMessage(),
               equalTo(initialEvent.getMessage()));
  }

  @Test
  public void compositePolicy() throws Exception {
    compositeSourcePolicy =
        new CompositeSourcePolicy(asList(firstPolicy, secondPolicy), sourcePolicyParametersTransformer,
                                  sourcePolicyProcessorFactory);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> sourcePolicyResult =
        from(compositeSourcePolicy.process(initialEvent, flowExecutionProcessor, sourceParametersProcessor)).block();
    assertThat(sourcePolicyResult.isRight(), is(true));
    assertThat(sourcePolicyResult.getRight().getResult().getMessage(), is(firstPolicyResultEvent.getMessage()));
    verify(flowExecutionProcessor).apply(publisherArgumentCaptor.capture());
    assertThat(((CoreEvent) from(publisherArgumentCaptor.getValue()).block()).getMessage(),
               equalTo(modifiedEvent.getMessage()));
    verify(sourcePolicyProcessorFactory).createSourcePolicy(same(firstPolicy), any());
    verify(sourcePolicyProcessorFactory).createSourcePolicy(same(secondPolicy), any());

    verify(firstPolicyProcessor).apply(publisherArgumentCaptor.capture());
    assertThat(firstPolicyActualResultEvent.getMessage(),
               equalTo(initialEvent.getMessage()));

    verify(secondPolicyProcessor).apply(publisherArgumentCaptor.capture());
    assertThat(secondPolicyActualResultEvent.getMessage(),
               equalTo(modifiedEvent.getMessage()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyPolicyList() throws Exception {
    compositeSourcePolicy = new CompositeSourcePolicy(emptyList(),
                                                      sourcePolicyParametersTransformer, sourcePolicyProcessorFactory);
  }

  @Test
  public void policyExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(sourcePolicyProcessorFactory.createSourcePolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(secondPolicyProcessor.apply(any(Publisher.class)))
          .thenAnswer(inv -> from((Publisher<CoreEvent>) inv.getArgument(0))
              // Internal policy exceptions are already mapped to messaging exceptions
              .flatMap(event -> error(new MessagingException(event, policyException))));
      return secondPolicyProcessor;
    });

    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy, secondPolicy), sourcePolicyParametersTransformer,
                                                      sourcePolicyProcessorFactory);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> sourcePolicyResult =
        from(compositeSourcePolicy.process(initialEvent, flowExecutionProcessor, sourceParametersProcessor)).block();
    assertThat(sourcePolicyResult.isLeft(), is(true));
    assertThat(sourcePolicyResult.getLeft().getMessagingException().getCause(), is(policyException));
  }

  @Test
  public void nextProcessorExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    reset(flowExecutionProcessor);
    when(flowExecutionProcessor.apply(any()))
        .thenAnswer(invocation -> from((Publisher<CoreEvent>) invocation.getArgument(0))
            // Flow exceptions are already mapped to messaging exceptions
            .flatMap(event -> error(new MessagingException(event, policyException))));
    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy, secondPolicy), sourcePolicyParametersTransformer,
                                                      sourcePolicyProcessorFactory);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> sourcePolicyResult =
        from(compositeSourcePolicy.process(initialEvent, flowExecutionProcessor, sourceParametersProcessor)).block();
    assertThat(sourcePolicyResult.isLeft(), is(true));
    assertThat(sourcePolicyResult.getLeft().getMessagingException().getCause(), is(policyException));
  }

  @Test
  public void reactorPipelinesBuiltJustOnce() {
    InvocationsRecordingCompositeSourcePolicy.reset();
    final InvocationsRecordingCompositeSourcePolicy sourcePolicy =
        new InvocationsRecordingCompositeSourcePolicy(asList(firstPolicy),
                                                      sourcePolicyParametersTransformer,
                                                      sourcePolicyProcessorFactory);

    assertThat(sourcePolicy.getNextOperationCount(), is(1));
    assertThat(sourcePolicy.getPolicyCount(), is(1));

    from(sourcePolicy.process(initialEvent, flowExecutionProcessor, sourceParametersProcessor)).block();

    assertThat(sourcePolicy.getNextOperationCount(), is(1));
    assertThat(sourcePolicy.getPolicyCount(), is(1));
  }

  public static final class InvocationsRecordingCompositeSourcePolicy extends CompositeSourcePolicy {

    private static final AtomicInteger nextOperation = new AtomicInteger();
    private static final AtomicInteger policy = new AtomicInteger();

    public InvocationsRecordingCompositeSourcePolicy(List<Policy> parameterizedPolicies,
                                                     Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                                                     SourcePolicyProcessorFactory sourcePolicyProcessorFactory) {
      super(parameterizedPolicies, sourcePolicyParametersTransformer, sourcePolicyProcessorFactory);
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
