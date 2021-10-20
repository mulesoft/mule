/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.EXECUTION_ENGINE;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.REACTOR;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

import org.junit.Test;
import reactor.core.publisher.FluxSink;

@Feature(EXECUTION_ENGINE)
@Story(REACTOR)
public class ReactorSinkProviderBasedSinkTestCase {

  private static final long GC_POLLING_TIMEOUT = 10000;

  @Test
  @Issue("MULE-19846")
  public void sinkCompletedAfterThreadTermination() throws InterruptedException {

    FluxSink<CoreEvent> fluxSink = (FluxSink<CoreEvent>) mock(FluxSink.class);
    ReactorSinkProvider sinkProvider = new AbstractCachedThreadReactorSinkProvider() {

      @Override
      protected FluxSink<CoreEvent> createSink() {
        return fluxSink;
      }
    };
    ReactorSinkProviderBasedSink reactorSinkProviderBasedSink = new ReactorSinkProviderBasedSink(sinkProvider);

    Thread thread = new Thread(() -> {
      reactorSinkProviderBasedSink.accept(mock(CoreEvent.class));
    });

    thread.start();

    PhantomReference<Thread> bindingValueRef = new PhantomReference<>(thread, new ReferenceQueue<>());

    thread.join();

    thread = null;

    new PollingProber(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(bindingValueRef.isEnqueued(), is(true));
      return true;
    }, "A hard reference is being mantained to the thread."));

    // we add another value to the cache so that the removal listener be called
    reactorSinkProviderBasedSink.accept(mock(CoreEvent.class));


    new PollingProber(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      verify(fluxSink).complete();
      return true;
    }, "The sink was not completed."));
  }
}
