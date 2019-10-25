/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.tck.probe.PollingProber.check;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.privileged.event.BaseEventContext;
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
import org.mockito.ArgumentCaptor;
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
  private BaseEventContext ctx;

  private CursorManager cursorManager;
  private ExecutorService executorService;

  @Before
  public void before() {
    cursorManager = new CursorManager(statistics, ghostBuster);
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
    assertThat(managedProviders.get(0), is(instanceOf(ManagedCursorProvider.class)));

    ManagedCursorProvider managedProvider = (ManagedCursorProvider) managedProviders.get(0);
    assertThat(managedProvider.getDelegate(), is(sameInstance(cursorProvider)));
    assertThat(managedProviders.stream().allMatch(p -> p == managedProvider), is(true));

    verify(ghostBuster).track(managedProvider);
  }

  @Test
  public void remanageCollectedDecorator() {
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    when(ghostBuster.track(any())).thenReturn(new WeakReference<>(null));

    cursorManager.manage(provider, ctx);

    ArgumentCaptor<ManagedCursorProvider> managedDecoratorCaptor = forClass(ManagedCursorProvider.class);
    verify(ghostBuster, times(2)).track(managedDecoratorCaptor.capture());

    List<ManagedCursorProvider> captured = managedDecoratorCaptor.getAllValues();
    assertThat(captured, hasSize(2));
    assertThat(captured.get(0), is(sameInstance(captured.get(1))));
  }
}
