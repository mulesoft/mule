/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.streaming.IdentifiableCursorProviderDecorator.of;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.tck.probe.PollingProber.check;

import static java.lang.System.gc;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import io.qameta.allure.Issue;
import org.junit.Test;

public class EventStreamingStateTestCase extends AbstractMuleContextTestCase {

  private static final int GC_POLLING_TIMEOUT = 10000;

  private EventStreamingState eventStreamingState;

  private StreamingGhostBuster ghostBuster;

  @Override
  protected void doSetUp() throws Exception {
    ghostBuster = new StreamingGhostBuster();
    initialiseIfNeeded(ghostBuster, true, muleContext);
    startIfNeeded(ghostBuster);
    eventStreamingState = new EventStreamingState();
  }

  @Override
  protected void doTearDown() throws MuleException {
    ghostBuster.stop();
    ghostBuster.dispose();
  }

  @Test
  @Issue("W-13066586")
  public void invalidateInCacheWhenReferenceIsCollected() {
    MutableStreamingStatistics statistics = mock(MutableStreamingStatistics.class);
    IdentifiableCursorProvider provider = of(mock(CursorStreamProvider.class));
    int id = provider.getId();
    ManagedCursorStreamProvider managedCursorProvider = new ManagedCursorStreamProvider(provider, statistics);
    eventStreamingState.addProvider(managedCursorProvider, ghostBuster);

    // Force GC collection
    managedCursorProvider = null;

    check(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL, () -> {
      gc();
      assertNull(eventStreamingState.providers.getIfPresent(id));
      return true;
    });
  }

}
