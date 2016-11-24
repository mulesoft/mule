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
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
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
      of(mock(OperationPolicyParametersTransformer.class));
  private OperationParametersProcessor operationParametersProcessor = mock(OperationParametersProcessor.class);
  private Policy firstPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  private Policy secondPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  private Event initialEvent = mock(Event.class);
  private Event firstPolicyResultEvent = mock(Event.class);
  private Event secondPolicyResultEvent = mock(Event.class);
  private Processor nextProcessor = mock(Processor.class);
  private Event nextProcessResultEvent = mock(Event.class);
  private OperationPolicyFactory operationPolicyFactory = mock(OperationPolicyFactory.class, RETURNS_DEEP_STUBS);
  private OperationPolicy firstPolicyOperationPolicy = mock(OperationPolicy.class);
  private OperationPolicy secondPolicyOperationPolicy = mock(OperationPolicy.class);

  @Before
  public void setUp() throws Exception {
    when(nextProcessor.process(any())).thenReturn(nextProcessResultEvent);
    when(firstPolicy.getPolicyChain().process(any())).thenReturn(firstPolicyResultEvent);
    when(secondPolicy.getPolicyChain().process(any())).thenReturn(secondPolicyResultEvent);
    when(operationPolicyFactory.createOperationPolicy(firstPolicy, operationPolicyParametersTransformer))
        .thenReturn(firstPolicyOperationPolicy);
    when(operationPolicyFactory.createOperationPolicy(secondPolicy, operationPolicyParametersTransformer))
        .thenReturn(secondPolicyOperationPolicy);
    when(firstPolicyOperationPolicy.process(any(Event.class), any(Processor.class), same(operationParametersProcessor)))
        .then(invocationOnMock -> {
          ((Processor) invocationOnMock.getArguments()[1])
              .process((Event) invocationOnMock.getArguments()[0]);
          return firstPolicyResultEvent;
        });
    when(secondPolicyOperationPolicy.process(any(Event.class), any(Processor.class), same(operationParametersProcessor)))
        .then(invocationOnMock -> {
          ((Processor) invocationOnMock.getArguments()[1])
              .process((Event) invocationOnMock.getArguments()[0]);
          return secondPolicyResultEvent;
        });
  }

  @Test
  public void singlePolicy() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyFactory);

    Event result = compositeOperationPolicy.process(initialEvent, nextProcessor, operationParametersProcessor);
    assertThat(result, is(nextProcessResultEvent));
    verify(nextProcessor).process(initialEvent);
    verify(operationPolicyFactory).createOperationPolicy(firstPolicy, operationPolicyParametersTransformer);
    verify(firstPolicyOperationPolicy).process(same(initialEvent), any(), same(operationParametersProcessor));
  }

  @Test
  public void compositePolicy() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyFactory);

    Event result = compositeOperationPolicy.process(initialEvent, nextProcessor, operationParametersProcessor);
    assertThat(result, is(nextProcessResultEvent));
    verify(nextProcessor).process(initialEvent);
    verify(operationPolicyFactory).createOperationPolicy(firstPolicy, operationPolicyParametersTransformer);
    verify(operationPolicyFactory).createOperationPolicy(secondPolicy, operationPolicyParametersTransformer);
    verify(firstPolicyOperationPolicy).process(same(initialEvent), any(), same(operationParametersProcessor));
    verify(firstPolicyOperationPolicy).process(same(initialEvent), any(), same(operationParametersProcessor));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyPolicyList() throws Exception {
    compositeOperationPolicy = new CompositeOperationPolicy(emptyList(),
                                                            operationPolicyParametersTransformer, operationPolicyFactory);
  }

  @Test
  public void policyExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(firstPolicyOperationPolicy.process(any(Event.class), any(Processor.class), any(OperationParametersProcessor.class)))
        .thenThrow(policyException);
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyFactory);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    compositeOperationPolicy.process(initialEvent, nextProcessor, operationParametersProcessor);
  }

  @Test
  public void nextProcessorExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(nextProcessor.process(any(Event.class))).thenThrow(policyException);
    compositeOperationPolicy = new CompositeOperationPolicy(asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer, operationPolicyFactory);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    compositeOperationPolicy.process(initialEvent, nextProcessor, operationParametersProcessor);
  }

}
