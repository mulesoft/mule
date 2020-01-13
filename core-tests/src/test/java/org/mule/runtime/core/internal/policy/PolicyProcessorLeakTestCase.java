/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;

public class PolicyProcessorLeakTestCase extends AbstractMuleTestCase {

  private static final int GC_POLLING_TIMEOUT = 10000;

  private Policy policy;

  @Before
  public void before() throws MuleException {
    final PolicyChain policyChain = new PolicyChain() {

      @Override
      public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
        // Skip the 'next' call
        return publisher;
      }
    };
    policy = new Policy(policyChain, "policyId");
  }

  @Override
  protected CoreEvent testEvent() throws MuleException {
    CoreEvent event = super.testEvent();
    ((InternalEvent) event).setSourcePolicyContext(new SourcePolicyContext(mock(PolicyPointcutParameters.class)));
    ((InternalEvent) event).setOperationPolicyContext(mock(OperationPolicyContext.class, RETURNS_DEEP_STUBS));
    return event;
  }

  @Test
  public void sourceNextOperationRefCleared() throws MuleException {
    Processor nextProcessor = new TestProcessor();
    final PhantomReference<Processor> processorRef = new PhantomReference<>(nextProcessor, new ReferenceQueue<>());
    SourcePolicyProcessor policyProcessor = new SourcePolicyProcessor(policy, nextProcessor);

    final CoreEvent event = testEvent();
    try {
      just(event).transform(policyProcessor).block();
      nextProcessor = null;
      policyProcessor = null;
      policy = null;
    } finally {
      ((BaseEventContext) event.getContext()).success();
    }

    probeNextRefGcd(processorRef);
  }

  @Test
  public void operationNextOperationRefCleared() throws MuleException {
    Processor nextProcessor = new TestProcessor();
    final PhantomReference<Processor> processorRef = new PhantomReference<>(nextProcessor, new ReferenceQueue<>());
    OperationPolicyProcessor policyProcessor = new OperationPolicyProcessor(policy, nextProcessor);

    final CoreEvent event = testEvent();
    try {
      just(event).transform(policyProcessor).block();
      nextProcessor = null;
      policyProcessor = null;
      policy = null;
    } finally {
      ((BaseEventContext) event.getContext()).success();
    }

    probeNextRefGcd(processorRef);
  }

  private void probeNextRefGcd(final PhantomReference<Processor> processorRef) {
    new PollingProber(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(processorRef.isEnqueued(), is(true));
      return true;
    }, "A hard reference is being mantained to the next processor."));
  }

  private static final class TestProcessor implements Processor {

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return publisher;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return event;
    }
  }
}
