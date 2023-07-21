/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;

import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.concurrent.atomic.AtomicInteger;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@Feature(STREAMING)
@RunWith(MockitoJUnitRunner.class)
public class CursorProviderJanitorTestCase extends AbstractMuleTestCase {

  @Mock
  private MutableStreamingStatistics statistics;

  @Mock
  private Cursor cursor;

  @Mock
  private CursorProvider provider;

  private CursorProviderJanitor janitor;
  private AtomicInteger openCursorsCounter = new AtomicInteger(0);

  @Before
  public void before() {
    janitor = new CursorProviderJanitor(provider, openCursorsCounter, statistics);
  }

  @Test
  public void resourcesReleasedOnJanitorRelease() {
    janitor.releaseResources();
    assertThat(janitor.provider, is(nullValue()));
    verify(provider).releaseResources();
  }

  @Test
  public void releaseSingleCursorOnOpenProvider() {
    when(provider.isClosed()).thenReturn(false);
    openCursorsCounter.set(2);
    janitor.releaseCursor(cursor);

    verify(cursor).release();
    assertThat(openCursorsCounter.get(), is(1));
    verify(provider, never()).releaseResources();
  }

  @Test
  public void releaseUniqueCursorOnOpenProvider() {
    when(provider.isClosed()).thenReturn(false);
    openCursorsCounter.set(1);
    janitor.releaseCursor(cursor);

    verify(cursor).release();
    assertThat(openCursorsCounter.get(), is(0));
    verify(provider, never()).releaseResources();
  }

  @Test
  public void releaseUniqueCursorOnClosedProvider() {
    when(provider.isClosed()).thenReturn(true);
    openCursorsCounter.set(1);
    janitor.releaseCursor(cursor);

    verify(cursor).release();
    assertThat(openCursorsCounter.get(), is(0));
    verify(provider).releaseResources();
  }

  @Test
  @Issue("MULE-18371")
  public void nestedCursors() {
    Cursor parentCursor = mock(Cursor.class);
    CursorProvider parentProvider = mock(CursorProvider.class);
    MutableStreamingStatistics parentStatistics = mock(MutableStreamingStatistics.class);
    AtomicInteger parentCursorCounter = new AtomicInteger(1);
    CursorProviderJanitor parentJanitor = new CursorProviderJanitor(parentProvider, parentCursorCounter, parentStatistics);

    parentJanitor.releaseResources();

    doAnswer(invocation -> {
      parentJanitor.releaseCursor(parentCursor);
      return null;
    }).when(cursor).release();

    janitor.releaseCursor(cursor);

    assertThat(parentCursorCounter.get(), is(0));
    verify(parentProvider).releaseResources();

    verify(cursor).release();
  }
}
