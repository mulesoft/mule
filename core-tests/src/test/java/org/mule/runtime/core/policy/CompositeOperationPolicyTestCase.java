/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

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
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
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
  private Event initialEvent;
  private Event firstPolicyProcessorResultEvent;
  private Event secondPolicyResultProcessorEvent;
  private OperationExecutionFunction operationExecutionFunction = mock(OperationExecutionFunction.class);
  private Event nextProcessResultEvent;

  private OperationPolicyProcessorFactory operationPolicyProcessorFactory = mock(OperationPolicyProcessorFactory.class);
  private Processor firstPolicyOperationPolicyProcessor = mock(Processor.class);
  private Processor secondPolicyOperationPolicyProcessor = mock(Processor.class);

  @Before
  public void setUp() throws Exception {
    initialEvent = createTestEvent();
    firstPolicyProcessorResultEvent = createTestEvent();
    secondPolicyResultProcessorEvent = createTestEvent();
    nextProcessResultEvent = createTestEvent();
    when(operationPolicyParametersTransformer.get().fromParametersToMessage(any())).thenReturn(Message.of(null));
    when(operationExecutionFunction.execute(any(), any())).thenReturn(nextProcessResultEvent);
    when(firstPolicy.getPolicyChain().process(any())).thenReturn(firstPolicyProcessorResultEvent);
    when(secondPolicy.getPolicyChain().process(any())).thenReturn(secondPolicyResultProcessorEvent);
    when(operationPolicyProcessorFactory.createOperationPolicy(same(secondPolicy), any()))
        .thenReturn(secondPolicyOperationPolicyProcessor);
    when(operationPolicyProcessorFactory.createOperationPolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(firstPolicyOperationPolicyProcessor.process(any())).thenAnswer(policyProcessorInvocation -> {
        ((Processor) policyFactoryInvocation.getArguments()[1]).process(initialEvent);
        return firstPolicyProcessorResultEvent;
      });
      return firstPolicyOperationPolicyProcessor;
    });
    when(operationPolicyProcessorFactory.createOperationPolicy(same(secondPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      when(secondPolicyOperationPolicyProcessor.process(any())).thenAnswer(policyProcessorInvocation -> {
        ((Processor) policyFactoryInvocation.getArguments()[1]).process(initialEvent);
        return secondPolicyResultProcessorEvent;
      });
      return secondPolicyOperationPolicyProcessor;
    });
  }

  @Test
  public void singlePolicy() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyProcessorFactory,
                                                            operationParametersProcessor, operationExecutionFunction);

    Event result = compositeOperationPolicy.process(initialEvent);
    assertThat(result, is(nextProcessResultEvent));
    verify(operationExecutionFunction).execute(any(), same(initialEvent));
    verify(operationPolicyProcessorFactory).createOperationPolicy(same(firstPolicy), any());
    verify(firstPolicyOperationPolicyProcessor).process(any());
  }

  @Test
  public void compositePolicy() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyProcessorFactory,
                                                            operationParametersProcessor, operationExecutionFunction);

    Event result = compositeOperationPolicy.process(initialEvent);
    assertThat(result, is(nextProcessResultEvent));
    verify(operationExecutionFunction).execute(any(), same(initialEvent));
    verify(operationPolicyProcessorFactory).createOperationPolicy(same(firstPolicy), any());
    verify(operationPolicyProcessorFactory).createOperationPolicy(same(secondPolicy), any());
    verify(firstPolicyOperationPolicyProcessor).process(any());
    verify(firstPolicyOperationPolicyProcessor).process(any());
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
    when(firstPolicyOperationPolicyProcessor.process(any(Event.class))).thenThrow(policyException);
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyProcessorFactory,
                                                            operationParametersProcessor, operationExecutionFunction);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    compositeOperationPolicy.process(initialEvent);
  }

  @Test
  public void nextProcessorExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(operationExecutionFunction.execute(any(), any())).thenThrow(policyException);
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyProcessorFactory,
                                                            operationParametersProcessor, operationExecutionFunction);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    compositeOperationPolicy.process(initialEvent);
  }

  private Event createTestEvent() {
    return Event.builder(DefaultEventContext.create(mockFlowConstruct, fromSingleComponent("http"))).message(Message.of(null))
        .build();
  }

}
