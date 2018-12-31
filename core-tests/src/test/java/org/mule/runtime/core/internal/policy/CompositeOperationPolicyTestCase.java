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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class CompositeOperationPolicyTestCase extends AbstractCompositePolicyTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private CompositeOperationPolicy compositeOperationPolicy;

  private final Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer =
      of(mock(OperationPolicyParametersTransformer.class, RETURNS_DEEP_STUBS));
  private final OperationParametersProcessor operationParametersProcessor = mock(OperationParametersProcessor.class);

  private CoreEvent initialEvent;
  private final OperationExecutionFunction operationExecutionFunction = mock(OperationExecutionFunction.class);
  private CoreEvent nextProcessResultEvent;

  private final OperationPolicyProcessorFactory operationPolicyProcessorFactory = mock(OperationPolicyProcessorFactory.class);

  @Before
  public void setUp() throws Exception {
    initialEvent = createTestEvent();
    nextProcessResultEvent = CoreEvent.builder(createTestEvent()).message(Message.of("HELLO")).build();
    when(operationPolicyParametersTransformer.get().fromParametersToMessage(any())).thenReturn(Message.of(null));
    when(operationExecutionFunction.execute(any(), any())).thenAnswer(invocationOnMock -> just(nextProcessResultEvent));

    when(operationPolicyProcessorFactory.createOperationPolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      return firstPolicyProcessor(policyFactoryInvocation,
                                  e -> e,
                                  e -> e);
    });
    when(operationPolicyProcessorFactory.createOperationPolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      return secondPolicyProcessor(policyFactoryInvocation,
                                   e -> e,
                                   e -> e);
    });
  }

  @Test
  public void singlePolicy() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy),
                                                            operationPolicyParametersTransformer,
                                                            operationPolicyProcessorFactory);

    CoreEvent result =
        from(compositeOperationPolicy.process(initialEvent, operationExecutionFunction, operationParametersProcessor))
            .doOnNext(event1 -> System.out.println("FINAL " + event1.getMessage().getPayload().getValue())).block();

    assertThat(result.getMessage(), is(nextProcessResultEvent.getMessage()));
    verify(operationExecutionFunction).execute(any(), any());
    verify(operationPolicyProcessorFactory).createOperationPolicy(same(firstPolicy), any());

    assertThat(getFirstPolicyActualResultEvent(), not(nullValue()));
  }

  @Test
  public void compositePolicy() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer,
                                                            operationPolicyProcessorFactory);

    CoreEvent result =
        from(compositeOperationPolicy.process(initialEvent, operationExecutionFunction, operationParametersProcessor)).block();
    assertThat(result.getMessage(), is(nextProcessResultEvent.getMessage()));
    verify(operationExecutionFunction).execute(any(), any());
    verify(operationPolicyProcessorFactory).createOperationPolicy(same(firstPolicy), any());
    verify(operationPolicyProcessorFactory).createOperationPolicy(same(secondPolicy), any());

    assertThat(getFirstPolicyActualResultEvent(), not(nullValue()));
    assertThat(getSecondPolicyActualResultEvent(), not(nullValue()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyPolicyList() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(emptyList(),
                                                            operationPolicyParametersTransformer,
                                                            operationPolicyProcessorFactory);
  }

  @Test
  public void policyExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(operationPolicyProcessorFactory.createOperationPolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      Processor firstPolicyOperationPolicyProcessor = mock(Processor.class);
      when(firstPolicyOperationPolicyProcessor.apply(any())).thenReturn(error(policyException));
      return firstPolicyOperationPolicyProcessor;
    });
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer,
                                                            operationPolicyProcessorFactory);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    try {
      from(compositeOperationPolicy.process(initialEvent, operationExecutionFunction, operationParametersProcessor)).block();
    } catch (Throwable throwable) {
      throw rxExceptionToMuleException(throwable);
    }
  }

  @Test
  public void nextProcessorExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(operationExecutionFunction.execute(any(), any())).thenReturn(error(policyException));
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer,
                                                            operationPolicyProcessorFactory);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    try {
      from(compositeOperationPolicy.process(initialEvent, operationExecutionFunction, operationParametersProcessor)).block();
    } catch (Throwable throwable) {
      throw rxExceptionToMuleException(throwable);
    }
  }

  @Test
  public void reactorPipelinesReused() {
    InvocationsRecordingCompositeOperationPolicy.reset();
    final InvocationsRecordingCompositeOperationPolicy operationPolicy =
        new InvocationsRecordingCompositeOperationPolicy(asList(firstPolicy),
                                                         operationPolicyParametersTransformer,
                                                         operationPolicyProcessorFactory);

    assertThat(operationPolicy.getNextOperationCount(), is(getRuntime().availableProcessors()));
    assertThat(operationPolicy.getPolicyCount(), is(getRuntime().availableProcessors()));

    for (int i = 0; i < getRuntime().availableProcessors() * 2; ++i) {
      from(operationPolicy.process(initialEvent, operationExecutionFunction, operationParametersProcessor)).block();
    }

    assertThat(operationPolicy.getNextOperationCount(), is(getRuntime().availableProcessors()));
    assertThat(operationPolicy.getPolicyCount(), is(getRuntime().availableProcessors()));
  }

  public static final class InvocationsRecordingCompositeOperationPolicy extends CompositeOperationPolicy {

    private static final AtomicInteger nextOperation = new AtomicInteger();
    private static final AtomicInteger policy = new AtomicInteger();

    public InvocationsRecordingCompositeOperationPolicy(List<Policy> parameterizedPolicies,
                                                        Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                                        OperationPolicyProcessorFactory operationPolicyProcessorFactory) {
      super(parameterizedPolicies, operationPolicyParametersTransformer, operationPolicyProcessorFactory);
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
      InvocationsRecordingCompositeOperationPolicy.policy.incrementAndGet();
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
