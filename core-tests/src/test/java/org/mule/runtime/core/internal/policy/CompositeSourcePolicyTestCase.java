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
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.policy.CompositeSourcePolicy;
import org.mule.runtime.core.internal.policy.MessageSourceResponseParametersProcessor;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.internal.policy.SourcePolicyFailureResult;
import org.mule.runtime.core.internal.policy.SourcePolicyProcessorFactory;
import org.mule.runtime.core.internal.policy.SourcePolicySuccessResult;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

//TODO MULE-10927 - create a common class between CompositeOperationPolicyTestCase and CompositeSourcePolicyTestCase
public class CompositeSourcePolicyTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private CompositeSourcePolicy compositeSourcePolicy;

  private FlowConstruct mockFlowConstruct = mock(FlowConstruct.class, RETURNS_DEEP_STUBS);
  private Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer =
      of(mock(SourcePolicyParametersTransformer.class));
  private MessageSourceResponseParametersProcessor sourceParametersTransformer =
      mock(MessageSourceResponseParametersProcessor.class, RETURNS_DEEP_STUBS);
  private Policy firstPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  private Policy secondPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  private Event initialEvent;
  private Event modifiedEvent;
  private Event firstPolicyResultEvent;
  private Event secondPolicyResultEvent;
  private Processor flowExecutionProcessor = mock(Processor.class);
  private Event nextProcessResultEvent = mock(Event.class);
  private SourcePolicyProcessorFactory sourcePolicyProcessorFactory =
      mock(SourcePolicyProcessorFactory.class, RETURNS_DEEP_STUBS);
  private Processor firstPolicySourcePolicyProcessor = mock(Processor.class);
  private Processor secondPolicySourcePolicyProcessor = mock(Processor.class);
  private ArgumentCaptor<Publisher> publisherArgumentCaptor = forClass(Publisher.class);

  @Before
  public void setUp() throws Exception {
    initialEvent = createTestEvent();
    modifiedEvent = createTestEvent();
    firstPolicyResultEvent = createTestEvent();
    secondPolicyResultEvent = createTestEvent();

    when(nextProcessResultEvent.getMessage()).thenReturn(mock(Message.class));
    when(flowExecutionProcessor.apply(any())).thenAnswer(invocation -> {
      Mono<Event> mono = from(invocation.getArgumentAt(0, Publisher.class));
      return mono.doOnNext(event -> event.getInternalContext().success(event));
    });
    when(firstPolicy.getPolicyChain().apply(any())).thenReturn(just(firstPolicyResultEvent));
    when(secondPolicy.getPolicyChain().apply(any())).thenReturn(just(secondPolicyResultEvent));

    when(sourcePolicyProcessorFactory.createSourcePolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(firstPolicySourcePolicyProcessor.apply(any())).thenAnswer(policyProcessorInvocation -> {
        just(modifiedEvent).transform((Processor) policyFactoryInvocation.getArguments()[1]).block();
        return just(firstPolicyResultEvent);
      });
      return firstPolicySourcePolicyProcessor;
    });
    when(sourcePolicyProcessorFactory.createSourcePolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(secondPolicySourcePolicyProcessor.apply(any())).thenAnswer(policyProcessorInvocation -> {
        just(modifiedEvent).transform((Processor) policyFactoryInvocation.getArguments()[1]).block();
        return just(secondPolicyResultEvent);
      });
      return secondPolicySourcePolicyProcessor;
    });
  }

  @Test
  public void singlePolicy() throws Exception {
    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy),
                                                      sourcePolicyParametersTransformer, sourcePolicyProcessorFactory,
                                                      flowExecutionProcessor, sourceParametersTransformer);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> sourcePolicyResult =
        from(compositeSourcePolicy.process(initialEvent)).block();
    assertThat(sourcePolicyResult.isRight(), is(true));
    assertThat(sourcePolicyResult.getRight().getResult().getMessage(), is(firstPolicyResultEvent.getMessage()));
    verify(flowExecutionProcessor).apply(publisherArgumentCaptor.capture());
    assertThat(((Event) from(publisherArgumentCaptor.getValue()).block()).getMessage(), equalTo(modifiedEvent.getMessage()));
    verify(sourcePolicyProcessorFactory).createSourcePolicy(same(firstPolicy), any());
    verify(firstPolicySourcePolicyProcessor).apply(publisherArgumentCaptor.capture());
    assertThat(((Event) from(publisherArgumentCaptor.getValue()).block()).getMessage(), equalTo(initialEvent.getMessage()));
  }

  @Test
  public void compositePolicy() throws Exception {
    compositeSourcePolicy =
        new CompositeSourcePolicy(asList(firstPolicy, secondPolicy), sourcePolicyParametersTransformer,
                                  sourcePolicyProcessorFactory, flowExecutionProcessor, sourceParametersTransformer);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> sourcePolicyResult =
        from(compositeSourcePolicy.process(initialEvent)).block();
    assertThat(sourcePolicyResult.isRight(), is(true));
    assertThat(sourcePolicyResult.getRight().getResult().getMessage(), is(firstPolicyResultEvent.getMessage()));
    verify(flowExecutionProcessor).apply(publisherArgumentCaptor.capture());
    assertThat(((Event) from(publisherArgumentCaptor.getValue()).block()).getMessage(), equalTo(modifiedEvent.getMessage()));
    verify(sourcePolicyProcessorFactory).createSourcePolicy(same(firstPolicy), any());
    verify(sourcePolicyProcessorFactory).createSourcePolicy(same(secondPolicy), any());
    verify(firstPolicySourcePolicyProcessor).apply(publisherArgumentCaptor.capture());
    assertThat(((Event) from(publisherArgumentCaptor.getValue()).block()).getMessage(), equalTo(initialEvent.getMessage()));
    verify(secondPolicySourcePolicyProcessor).apply(publisherArgumentCaptor.capture());
    assertThat(((Event) from(publisherArgumentCaptor.getValue()).block()).getMessage(), equalTo(modifiedEvent.getMessage()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyPolicyList() throws Exception {
    compositeSourcePolicy = new CompositeSourcePolicy(emptyList(),
                                                      sourcePolicyParametersTransformer, sourcePolicyProcessorFactory,
                                                      flowExecutionProcessor, sourceParametersTransformer);
  }

  @Test
  public void policyExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(sourcePolicyProcessorFactory.createSourcePolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(secondPolicySourcePolicyProcessor.apply(any(Publisher.class))).thenReturn(error(policyException));
      return secondPolicySourcePolicyProcessor;
    });

    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy, secondPolicy), sourcePolicyParametersTransformer,
                                                      sourcePolicyProcessorFactory,
                                                      flowExecutionProcessor, sourceParametersTransformer);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    try {
      from(compositeSourcePolicy.process(initialEvent)).block();
    } catch (Throwable t) {
      throw rxExceptionToMuleException(t);
    }
  }

  @Test
  public void nextProcessorExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    reset(flowExecutionProcessor);
    when(flowExecutionProcessor.apply(any())).thenAnswer(invocation -> {
      Mono<Event> mono = from(invocation.getArgumentAt(0, Publisher.class));
      mono.doOnNext(event -> event.getInternalContext().error(policyException)).subscribe();
      return empty();
    });
    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy, secondPolicy), sourcePolicyParametersTransformer,
                                                      sourcePolicyProcessorFactory,
                                                      flowExecutionProcessor, sourceParametersTransformer);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    try {
      from(compositeSourcePolicy.process(initialEvent)).block();
    } catch (Throwable t) {
      throw rxExceptionToMuleException(t);
    }
  }

  private Event createTestEvent() {
    return Event.builder(DefaultEventContext.create(mockFlowConstruct, fromSingleComponent("http")))
        .message(Message.builder().nullValue().build()).build();
  }

}
