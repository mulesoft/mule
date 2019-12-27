/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.tck.probe.PollingProber.check;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.event.DefaultEventContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CursorManagerTestCase extends AbstractMuleTestCase {

  @Mock
  private MutableStreamingStatistics statistics;

  @Mock
  private StreamingGhostBuster ghostBuster;

  @Mock
  private CoreEvent event;

  @Mock
  private ComponentLocation location;

  private DefaultEventContext ctx;
  private CursorManager cursorManager;
  private ExecutorService executorService;

  @Before
  public void before() {
    cursorManager = new CursorManager(statistics, ghostBuster);
    ctx = new DefaultEventContext("id", "server", location, "", empty());
    when(ghostBuster.track(any())).thenAnswer(inv -> new WeakReference<>(inv.getArgument(0)));
  }

  @After
  public void after() {
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  @Test
  public void manageTheSameProviderMultipleTimesWithConcurrency() throws Exception {
    final int threadCount = 5;
    List<CursorProvider> managedProviders = new ArrayList<>(threadCount);
    Latch latch = new Latch();
    executorService = Executors.newFixedThreadPool(threadCount);

    final CursorProvider cursorProvider = mock(CursorStreamProvider.class);

    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try {
          latch.await();
        } catch (InterruptedException e) {
          // doesn't matter
        }
        CursorProvider managed = cursorManager.manage(cursorProvider, ctx);
        synchronized (managedProviders) {
          managedProviders.add(managed);
        }
      });
    }

    latch.release();

    check(5000, 100, () -> managedProviders.size() == threadCount);
    managedProviders.forEach(p -> {
      ManagedCursorProvider managedProvider = (ManagedCursorProvider) p;
      assertThat(managedProvider, is(instanceOf(ManagedCursorProvider.class)));
      assertThat(managedProvider.getDelegate(), is(sameInstance(cursorProvider)));

      verify(cursorProvider, never()).releaseResources();
      verify(cursorProvider, never()).close();
      verify(ghostBuster).track(managedProvider);
    });

    ctx.success();

    verify(cursorProvider).releaseResources();
    verify(cursorProvider).close();
  }
}
