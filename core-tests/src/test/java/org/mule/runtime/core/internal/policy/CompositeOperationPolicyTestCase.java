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
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

//TODO MULE-10927 - create a common class between CompositeOperationPolicyTestCase and CompositeSourcePolicyTestCase
public class CompositeOperationPolicyTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private CompositeOperationPolicy compositeOperationPolicy;

  private Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer =
      of(mock(OperationPolicyParametersTransformer.class, RETURNS_DEEP_STUBS));
  private OperationParametersProcessor operationParametersProcessor = mock(OperationParametersProcessor.class);
  private Policy firstPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  private Policy secondPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  private FlowConstruct mockFlowConstruct = mock(FlowConstruct.class, RETURNS_DEEP_STUBS);
  private CoreEvent initialEvent;
  private CoreEvent firstPolicyProcessorResultEvent;
  private CoreEvent secondPolicyResultProcessorEvent;
  private OperationExecutionFunction operationExecutionFunction = mock(OperationExecutionFunction.class);
  private CoreEvent nextProcessResultEvent;

  private OperationPolicyProcessorFactory operationPolicyProcessorFactory = mock(OperationPolicyProcessorFactory.class);
  private Processor firstPolicyOperationPolicyProcessor = mock(Processor.class);
  private Processor secondPolicyOperationPolicyProcessor = mock(Processor.class);

  @Before
  public void setUp() throws Exception {
    initialEvent = createTestEvent();
    firstPolicyProcessorResultEvent = createTestEvent();
    secondPolicyResultProcessorEvent = createTestEvent();
    nextProcessResultEvent = CoreEvent.builder(createTestEvent()).message(Message.of("HELLO")).build();
    when(operationPolicyParametersTransformer.get().fromParametersToMessage(any())).thenReturn(Message.of(null));
    when(operationExecutionFunction.execute(any(), any())).thenAnswer(invocationOnMock -> just(nextProcessResultEvent));
    when(firstPolicy.getPolicyChain().apply(any())).thenReturn(just(firstPolicyProcessorResultEvent));
    when(secondPolicy.getPolicyChain().apply(any())).thenReturn(just(secondPolicyResultProcessorEvent));
    when(operationPolicyProcessorFactory.createOperationPolicy(same(secondPolicy), any()))
        .thenReturn(secondPolicyOperationPolicyProcessor);
    when(operationPolicyProcessorFactory.createOperationPolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(firstPolicyOperationPolicyProcessor.apply(any())).thenAnswer(policyProcessorInvocation -> {
        just(initialEvent).transform((Processor) policyFactoryInvocation.getArguments()[1]).block();
        return just(firstPolicyProcessorResultEvent);
      });
      return firstPolicyOperationPolicyProcessor;
    });
    when(operationPolicyProcessorFactory.createOperationPolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(secondPolicyOperationPolicyProcessor.apply(any())).thenAnswer(policyProcessorInvocation -> {
        just(initialEvent).transform((Processor) policyFactoryInvocation.getArguments()[1]).block();
        return just(secondPolicyResultProcessorEvent);
      });
      return secondPolicyOperationPolicyProcessor;
    });
  }

  @Test
  public void singlePolicy() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyProcessorFactory,
                                                            operationParametersProcessor, operationExecutionFunction);

    CoreEvent result = from(compositeOperationPolicy.process(initialEvent))
        .doOnNext(event1 -> System.out.println("FINAL " + event1.getMessage().getPayload().getValue())).block();

    assertThat(result.getMessage(), is(nextProcessResultEvent.getMessage()));
    verify(operationExecutionFunction).execute(any(), same(initialEvent));
    verify(operationPolicyProcessorFactory).createOperationPolicy(same(firstPolicy), any());
    verify(firstPolicyOperationPolicyProcessor).apply(any());
  }

  @Test
  public void compositePolicy() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyProcessorFactory,
                                                            operationParametersProcessor, operationExecutionFunction);

    CoreEvent result = from(compositeOperationPolicy.process(initialEvent)).block();
    assertThat(result.getMessage(), is(nextProcessResultEvent.getMessage()));
    verify(operationExecutionFunction).execute(any(), same(initialEvent));
    verify(operationPolicyProcessorFactory).createOperationPolicy(same(firstPolicy), any());
    verify(operationPolicyProcessorFactory).createOperationPolicy(same(secondPolicy), any());
    verify(firstPolicyOperationPolicyProcessor).apply(any());
    verify(firstPolicyOperationPolicyProcessor).apply(any());
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyPolicyList() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(emptyList(),
                                                            operationPolicyParametersTransformer, operationPolicyProcessorFactory,
                                                            operationParametersProcessor, operationExecutionFunction);
  }

  @Test
  public void policyExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(operationPolicyProcessorFactory.createOperationPolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(firstPolicyOperationPolicyProcessor.apply(any())).thenReturn(error(policyException));
      return firstPolicyOperationPolicyProcessor;
    });
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyProcessorFactory,
                                                            operationParametersProcessor, operationExecutionFunction);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    try {
      from(compositeOperationPolicy.process(initialEvent)).block();
    } catch (Throwable throwable) {
      throw rxExceptionToMuleException(throwable);
    }
  }

  @Test
  public void nextProcessorExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(operationExecutionFunction.execute(any(), any())).thenReturn(error(policyException));
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyProcessorFactory,
                                                            operationParametersProcessor, operationExecutionFunction);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    try {
      from(compositeOperationPolicy.process(initialEvent)).block();
    } catch (Throwable throwable) {
      throw rxExceptionToMuleException(throwable);
    }
  }

  private CoreEvent createTestEvent() {
    return CoreEvent.builder(create(mockFlowConstruct, fromSingleComponent("http"))).message(Message.of(null)).build();
  }

}
