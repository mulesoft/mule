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
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

//TODO MULE-10927 - create a common class between CompositeOperationPolicyTestCase and CompositeSourcePolicyTestCase
public class CompositeSourcePolicyTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private CompositeSourcePolicy compositeSourcePolicy;

  private Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer =
      of(mock(SourcePolicyParametersTransformer.class));
  private MessageSourceResponseParametersProcessor sourceParametersTransformer =
      mock(MessageSourceResponseParametersProcessor.class);
  private Policy firstPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  private Policy secondPolicy = mock(Policy.class, RETURNS_DEEP_STUBS);
  private Event initialEvent = mock(Event.class);
  private Event firstPolicyResultEvent = mock(Event.class);
  private Event secondPolicyResultEvent = mock(Event.class);
  private Processor nextProcessor = mock(Processor.class);
  private Event nextProcessResultEvent = mock(Event.class);
  private SourcePolicyFactory sourcePolicyFactory = mock(SourcePolicyFactory.class, RETURNS_DEEP_STUBS);
  private SourcePolicy firstPolicySourcePolicy = mock(SourcePolicy.class);
  private SourcePolicy secondPolicySourcePolicy = mock(SourcePolicy.class);

  @Before
  public void setUp() throws Exception {
    when(nextProcessor.process(any())).thenReturn(nextProcessResultEvent);
    when(firstPolicy.getPolicyChain().process(any())).thenReturn(firstPolicyResultEvent);
    when(secondPolicy.getPolicyChain().process(any())).thenReturn(secondPolicyResultEvent);
    when(sourcePolicyFactory.createSourcePolicy(firstPolicy, sourcePolicyParametersTransformer))
        .thenReturn(firstPolicySourcePolicy);
    when(sourcePolicyFactory.createSourcePolicy(secondPolicy, sourcePolicyParametersTransformer))
        .thenReturn(secondPolicySourcePolicy);
    when(firstPolicySourcePolicy.process(any(Event.class), any(Processor.class), same(sourceParametersTransformer)))
        .then(invocationOnMock -> {
          ((Processor) invocationOnMock.getArguments()[1])
              .process((Event) invocationOnMock.getArguments()[0]);
          return firstPolicyResultEvent;
        });
    when(secondPolicySourcePolicy.process(any(Event.class), any(Processor.class), same(sourceParametersTransformer)))
        .then(invocationOnMock -> {
          ((Processor) invocationOnMock.getArguments()[1])
              .process((Event) invocationOnMock.getArguments()[0]);
          return secondPolicyResultEvent;
        });
  }

  @Test
  public void singlePolicy() throws Exception {
    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy),
                                                      sourcePolicyParametersTransformer, sourcePolicyFactory, flowExecutionProcessor);

    Event result = compositeSourcePolicy.process(initialEvent, nextProcessor, sourceParametersTransformer);
    assertThat(result, is(firstPolicyResultEvent));
    verify(nextProcessor).process(initialEvent);
    verify(sourcePolicyFactory).createSourcePolicy(firstPolicy, sourcePolicyParametersTransformer);
    verify(firstPolicySourcePolicy).process(same(initialEvent), any(), same(sourceParametersTransformer));
  }

  @Test
  public void compositePolicy() throws Exception {
    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy, secondPolicy),
                                                      sourcePolicyParametersTransformer, sourcePolicyFactory, flowExecutionProcessor);

    Event result = compositeSourcePolicy.process(initialEvent, nextProcessor, sourceParametersTransformer);
    assertThat(result, is(firstPolicyResultEvent));
    verify(nextProcessor).process(initialEvent);
    verify(sourcePolicyFactory).createSourcePolicy(firstPolicy, sourcePolicyParametersTransformer);
    verify(sourcePolicyFactory).createSourcePolicy(secondPolicy, sourcePolicyParametersTransformer);
    verify(firstPolicySourcePolicy).process(same(initialEvent), any(), same(sourceParametersTransformer));
    verify(firstPolicySourcePolicy).process(same(initialEvent), any(), same(sourceParametersTransformer));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyPolicyList() throws Exception {
    compositeSourcePolicy = new CompositeSourcePolicy(emptyList(),
                                                      sourcePolicyParametersTransformer, sourcePolicyFactory, flowExecutionProcessor);
  }

  @Test
  public void policyExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(firstPolicySourcePolicy.process(any(Event.class), any(Processor.class),
                                         any(MessageSourceResponseParametersProcessor.class))).thenThrow(policyException);
    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy, secondPolicy),
                                                      sourcePolicyParametersTransformer, sourcePolicyFactory, flowExecutionProcessor);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    compositeSourcePolicy.process(initialEvent, nextProcessor, sourceParametersTransformer);
  }

  @Test
  public void nextProcessorExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(nextProcessor.process(any(Event.class))).thenThrow(policyException);
    compositeSourcePolicy = new CompositeSourcePolicy(asList(firstPolicy, secondPolicy),
                                                      sourcePolicyParametersTransformer, sourcePolicyFactory, flowExecutionProcessor);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    compositeSourcePolicy.process(initialEvent, nextProcessor, sourceParametersTransformer);
  }

}
