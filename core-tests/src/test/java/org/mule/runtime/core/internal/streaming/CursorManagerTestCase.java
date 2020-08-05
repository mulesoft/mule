/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static com.google.common.collect.Lists.partition;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.tck.probe.PollingProber.check;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

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
import java.util.stream.IntStream;

import io.qameta.allure.Issue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@SmallTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(CursorUtils.class)
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
  @Issue("MULE-17687")
  public void manageTheSameProviderMultipleTimesWithConcurrency() {
    final int threadCount = 5;
    List<CursorProvider> managedProviders = new ArrayList<>(threadCount);
    Latch latch = new Latch();
    executorService = newFixedThreadPool(threadCount);

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
  @Issue("MULE-17687")
  public void remanageCollectedDecorator() {
    // Setup collected decorator
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    when(ghostBuster.track(any())).thenReturn(new WeakReference<>(null));
    CursorProvider collectedCursorProvider = cursorManager.manage(provider, ctx);

    // Setup ghost buster
    when(ghostBuster.track(any())).thenAnswer(inv -> new WeakReference<>(inv.getArgument(0)));

    CursorProvider remanaged = cursorManager.manage(provider, ctx);

    ArgumentCaptor<ManagedCursorProvider> managedDecoratorCaptor = forClass(ManagedCursorProvider.class);
    verify(ghostBuster, times(2)).track(managedDecoratorCaptor.capture());

    List<ManagedCursorProvider> captured = managedDecoratorCaptor.getAllValues();
    assertThat(captured, hasSize(2));
    assertThat(captured.get(1), is(sameInstance(remanaged)));
    assertThat(captured.get(1).getDelegate(), is(sameInstance(provider)));
  }

  @Test
  @Issue("MULE-18573")
  public void manageTwoDifferentCursorProviderWithSameKey() {
    // setup all cursor provider generate same key
    mockStatic(CursorUtils.class);
    given(CursorUtils.managedCursorProviderKey(any())).willReturn(10);

    CursorStreamProvider firstProvider = mock(CursorStreamProvider.class);
    CursorProvider managed = cursorManager.manage(firstProvider, ctx);

    CursorStreamProvider secondProvider = mock(CursorStreamProvider.class);
    CursorProvider managedSecond = cursorManager.manage(secondProvider, ctx);

    ArgumentCaptor<ManagedCursorProvider> managedDecoratorCaptor = forClass(ManagedCursorProvider.class);
    verify(ghostBuster, times(2)).track(managedDecoratorCaptor.capture());

    List<ManagedCursorProvider> captured = managedDecoratorCaptor.getAllValues();
    assertThat(captured, hasSize(2));
    assertThat(captured.get(0), is(sameInstance(managed)));
    assertThat(captured.get(0).getDelegate(), is(sameInstance(firstProvider)));
    assertThat(captured.get(1), is(sameInstance(managedSecond)));
    assertThat(captured.get(1).getDelegate(), is(sameInstance(secondProvider)));
  }

  @Test
  public void manageThreeDifferentCursorProvidersWithSameKeyMultipleTimesWithConcurrency() {
    // setup all cursor provider generate same key
    mockStatic(CursorUtils.class);
    given(CursorUtils.managedCursorProviderKey(any())).willReturn(10);

    final int threadCount = 12;
    List<CursorProvider> managedProviders = new ArrayList<>(threadCount);
    Latch latch = new Latch();
    executorService = newFixedThreadPool(threadCount);

    List<CursorProvider> providers = new ArrayList<>();
    providers.add(mock(CursorStreamProvider.class));
    providers.add(mock(CursorStreamProvider.class));
    providers.add(mock(CursorStreamProvider.class));

    IntStream threads = IntStream.range(0, threadCount);
    List<List<Integer>> threadsGroups = partition(threads.boxed().collect(toList()), providers.size());

    for (List<Integer> threadGroups : threadsGroups) {
      for (int i = 0; i < threadGroups.size(); i++) {
        CursorProvider cursorProvider = providers.get(i);
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
    }

    latch.release();

    check(5000, 100, () -> managedProviders.size() == threadCount);
    assertThat(managedProviders.get(0), is(instanceOf(ManagedCursorProvider.class)));

    ArgumentCaptor<ManagedCursorProvider> managedDecoratorCaptor = forClass(ManagedCursorProvider.class);
    verify(ghostBuster, times(3)).track(managedDecoratorCaptor.capture());

    assertThat(managedProviders.get(0), is(instanceOf(ManagedCursorProvider.class)));
    assertThat(managedProviders.get(1), is(instanceOf(ManagedCursorProvider.class)));
    assertThat(managedProviders.get(2), is(instanceOf(ManagedCursorProvider.class)));

    List<ManagedCursorProvider> captured = managedDecoratorCaptor.getAllValues();
    assertThat(captured, hasSize(3));

    for (ManagedCursorProvider managedCursorProvider : captured) {
      assertThat(providers, hasItem(managedCursorProvider.getDelegate()));
      providers.remove(managedCursorProvider.getDelegate());
    }
    assertThat(providers.size(), is(0));
  }
}
