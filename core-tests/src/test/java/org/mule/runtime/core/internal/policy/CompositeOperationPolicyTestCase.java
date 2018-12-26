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
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
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
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.function.Consumer;

public class CompositeOperationPolicyTestCase extends AbstractCompositePolicyTestCase {

  private CompositeOperationPolicy compositeOperationPolicy;

  private final Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer =
      of(mock(OperationPolicyParametersTransformer.class, RETURNS_DEEP_STUBS));
  private final OperationParametersProcessor operationParametersProcessor = mock(OperationParametersProcessor.class);

  private final OperationExecutionFunction operationExecutionFunction = mock(OperationExecutionFunction.class);

  private final OperationPolicyProcessorFactory operationPolicyProcessorFactory = mock(OperationPolicyProcessorFactory.class);

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    when(operationPolicyParametersTransformer.get().fromParametersToMessage(any())).thenReturn(Message.of(null));
    when(operationExecutionFunction.execute(any(), any()))
        .thenAnswer(operationExecInv -> just((CoreEvent) operationExecInv.getArgument(1))
            .map(e -> CoreEvent.builder(e).message(nextProcessResultEvent.getMessage()).build()));
    when(operationPolicyProcessorFactory.createOperationPolicy(same(secondPolicy), any()))
        .thenReturn(secondPolicyProcessor);

    ////
    when(operationPolicyProcessorFactory.createOperationPolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(firstPolicyProcessor.apply(any()))
          .thenAnswer(policyProcessorInvocation -> from((Publisher<CoreEvent>) policyProcessorInvocation.getArguments()[0])
              .transform((ReactiveProcessor) policyFactoryInvocation.getArguments()[1]));
      return firstPolicyProcessor;
    });
    when(operationPolicyProcessorFactory.createOperationPolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(secondPolicyProcessor.apply(any()))
          .thenAnswer(policyProcessorInvocation -> from((Publisher<CoreEvent>) policyProcessorInvocation.getArguments()[0])
              .transform((ReactiveProcessor) policyFactoryInvocation.getArguments()[1]));
      return secondPolicyProcessor;
    });
    ///

    // when(operationPolicyProcessorFactory.createOperationPolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation ->
    // {
    // when(firstPolicyProcessor.apply(any())).thenAnswer(firstPolicyProcessorAnswer(policyFactoryInvocation, ev -> {
    // }));
    // return firstPolicyProcessor;
    // });
    // when(operationPolicyProcessorFactory.createOperationPolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation
    // -> {
    // when(secondPolicyProcessor.apply(any())).thenAnswer(secondPolicyProcessorAnswer(policyFactoryInvocation, ev -> {
    // }));
    // return secondPolicyProcessor;
    // });
  }

  private Answer<?> firstPolicyProcessorAnswer(InvocationOnMock policyFactoryInvocation,
                                               Consumer<CoreEvent> eventInputCallback) {
    return policyProcessorInvocation -> from((Publisher<CoreEvent>) policyProcessorInvocation.getArguments()[0])
        .doOnNext(eventInputCallback)
        // .map(e -> CoreEvent.builder(e).message(modifiedEvent.getMessage()).build())
        .transform((ReactiveProcessor) policyFactoryInvocation.getArguments()[1])
        .map(e -> CoreEvent.builder(e).message(firstPolicyResultEvent.getMessage()).build());
  }

  private Answer<?> secondPolicyProcessorAnswer(InvocationOnMock policyFactoryInvocation,
                                                Consumer<CoreEvent> eventInputCallback) {
    return policyProcessorInvocation -> from((Publisher<CoreEvent>) policyProcessorInvocation.getArguments()[0])
        .doOnNext(eventInputCallback)
        // .map(e -> CoreEvent.builder(e).message(modifiedEvent.getMessage()).build())
        .transform((ReactiveProcessor) policyFactoryInvocation.getArguments()[1])
        .map(e -> CoreEvent.builder(e).message(secondPolicyResultEvent.getMessage()).build());
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
    verify(firstPolicyProcessor).apply(any());
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
    verify(firstPolicyProcessor).apply(any());
    verify(firstPolicyProcessor).apply(any());
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
      when(firstPolicyProcessor.apply(any()))
          .thenAnswer(invocation -> from((Publisher<CoreEvent>) invocation.getArgument(0))
              // Internal policy exceptions are already mapped to messaging exceptions
              .flatMap(event -> error(new MessagingException(event, policyException))));
      return firstPolicyProcessor;
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
    doAnswer(invocation -> just((CoreEvent) invocation.getArgument(1))
        // Internal operation exceptions are already mapped to messaging exceptions
        .flatMap(event -> error(new MessagingException(event, policyException))))
            .when(operationExecutionFunction).execute(any(), any());

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

}
