/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;

import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.concurrent.atomic.AtomicInteger;

import io.qameta.allure.Feature;
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
}
