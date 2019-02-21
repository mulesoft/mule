/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Feature;

@Feature(STREAMING)
public class CursorProviderJanitorTestCase extends AbstractMuleTestCase {

  @Test
  public void resourcesReleasedOnJanitorRelease() {
    CursorProvider provider = mock(CursorProvider.class);
    MutableStreamingStatistics statistics = mock(MutableStreamingStatistics.class);
    Set<Cursor> cursors = new HashSet<Cursor>();
    Cursor cursor = mock(Cursor.class);
    cursors.add(cursor);
    CursorProviderJanitor janitor = new CursorProviderJanitor(provider, cursors, statistics);
    janitor.releaseResources();
    assertThat(janitor.provider, is(nullValue()));
    assertThat(cursors, is(empty()));
    verify(cursor).release();
    verify(provider).releaseResources();
  }

}
