/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.core.api.alert.MuleAlertingSupport.AlertNames.ALERT_NOT_CONSUMED_STREAM_BUSTED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.streaming.IdentifiableCursorProviderDecorator.of;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STREAM_MANAGEMENT;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SUPPORTABILITY;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SupportabilityStory.ALERTS;

import static java.lang.System.gc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.alert.AlertingSupport;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.internal.streaming.StreamingGhostBuster.StreamingWeakReference;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.ref.WeakReference;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(STREAM_MANAGEMENT)
public class StreamingGhostBusterTestCase extends AbstractMuleTestCase {

  private static final int GC_POLLING_TIMEOUT = 10000;

  private AlertingSupport alertingSupport;

  private StreamingGhostBuster ghostBuster;

  @BeforeEach
  public void setUp() throws Exception {
    alertingSupport = mock(AlertingSupport.class);
    ghostBuster = new StreamingGhostBuster();
    ghostBuster.setSchedulerService(new SimpleUnitTestSupportSchedulerService());
    ghostBuster.setAlertingSupport(alertingSupport);
    initialiseIfNeeded(ghostBuster);
    startIfNeeded(ghostBuster);
  }

  @AfterEach
  public void tearDown() throws MuleException {
    ghostBuster.stop();
    ghostBuster.dispose();
  }

  @Test
  @Issue("MULE-18573")
  public void releaseResourcesWhenReferenceIsCollected() {
    MutableStreamingStatistics statistics = mock(MutableStreamingStatistics.class);
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    ManagedCursorStreamProvider managedCursorProvider = new ManagedCursorStreamProvider(of(provider), statistics);

    WeakReference<ManagedCursorProvider> reference = ghostBuster.track(managedCursorProvider);

    // Force GC collection
    managedCursorProvider = null;

    check(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL, () -> {
      gc();
      assertThat(reference.get(), is(nullValue()));
      verify(provider).releaseResources();
      return true;
    });
  }

  @Test
  @Feature(SUPPORTABILITY)
  @Story(ALERTS)
  public void collectedReferenceNotClosedTriggersAlert() {
    MutableStreamingStatistics statistics = mock(MutableStreamingStatistics.class);
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    when(provider.getOriginatingLocation()).thenReturn(Optional.of(from("streamGenerator")));
    ManagedCursorStreamProvider managedCursorProvider = new ManagedCursorStreamProvider(of(provider), statistics);

    WeakReference<ManagedCursorProvider> reference = ghostBuster.track(managedCursorProvider);

    // Force GC collection
    managedCursorProvider = null;

    check(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL, () -> {
      gc();
      assertThat(reference.get(), is(nullValue()));
      verify(alertingSupport).triggerAlert(ALERT_NOT_CONSUMED_STREAM_BUSTED, "streamGenerator");
      return true;
    });
  }

  @Test
  @Feature(SUPPORTABILITY)
  @Story(ALERTS)
  public void collectedReferencePreviouslyClosedNotTriggersAlert() {
    MutableStreamingStatistics statistics = mock(MutableStreamingStatistics.class);
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    when(provider.getOriginatingLocation()).thenReturn(Optional.of(from("streamGenerator")));
    ManagedCursorStreamProvider managedCursorProvider = new ManagedCursorStreamProvider(of(provider), statistics);
    managedCursorProvider.releaseResources();

    StreamingWeakReference reference = (StreamingWeakReference) ghostBuster.track(managedCursorProvider);
    reference.dispose();
    verify(alertingSupport, never()).triggerAlert(any());
    verify(alertingSupport, never()).triggerAlert(any(), any());
  }

}
