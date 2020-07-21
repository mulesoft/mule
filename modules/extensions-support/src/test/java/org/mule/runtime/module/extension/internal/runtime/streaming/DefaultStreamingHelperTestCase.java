/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.object.factory.InMemoryCursorIteratorProviderFactory;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import io.qameta.allure.Issue;
import org.junit.Test;

@SmallTest
public class DefaultStreamingHelperTestCase extends AbstractMuleContextTestCase {

  private StreamingHelper streamingHelper;
  private StreamingManager streamingManager;
  private CursorProviderFactory cursorProviderFactory;
  private CoreEvent event;

  private List<String> valueList = Arrays.asList("Apple", "Banana", "Kiwi");

  @Override
  protected void doSetUp() throws Exception {
    streamingManager = new DefaultStreamingManager();
    initialiseIfNeeded(streamingManager, true, muleContext);
    cursorProviderFactory =
        new InMemoryCursorIteratorProviderFactory(InMemoryCursorIteratorConfig.getDefault(), streamingManager);
    event = testEvent();
    streamingHelper = new DefaultStreamingHelper(cursorProviderFactory, streamingManager, event);
  }

  @Override
  protected void doTearDown() throws Exception {
    if (streamingManager != null) {
      ((Disposable) streamingManager).dispose();
    }
  }

  @Test
  public void resolveIteratorProvider() {
    CursorIteratorProvider streamProvider = (CursorIteratorProvider) streamingHelper.resolveCursorProvider(valueList.iterator());
    CursorIterator cursor = streamProvider.openCursor();

    valueList.forEach(value -> {
      assertThat(cursor.hasNext(), is(true));
      assertThat(value, equalTo(cursor.next()));
    });

    assertThat(cursor.hasNext(), is(false));
  }

  @Test
  @Issue("MULE-18584")
  public void streamingStatisticsOnOpenAndClose() throws IOException {
    InputStream stream = new ByteArrayInputStream("Apple".getBytes());
    ManagedCursorStreamProvider streamProvider = (ManagedCursorStreamProvider) streamingHelper.resolveCursorProvider(stream);

    CursorStream cursorStream = streamProvider.openCursor();
    assertThat(streamingManager.getStreamingStatistics().getOpenCursorsCount(), is(1));

    cursorStream.close();
    assertThat(streamingManager.getStreamingStatistics().getOpenCursorsCount(), is(0));

    CursorStream cursorStream1 = streamProvider.openCursor();
    CursorStream cursorStream2 = streamProvider.openCursor();
    CursorStream cursorStream3 = streamProvider.openCursor();
    assertThat(streamingManager.getStreamingStatistics().getOpenCursorsCount(), is(3));

    cursorStream1.close();
    cursorStream2.close();
    cursorStream3.close();
    assertThat(streamingManager.getStreamingStatistics().getOpenCursorsCount(), is(0));
  }

  @Test
  public void resolveStreamableTypedValueProvider() {
    TypedValue typedValue = new TypedValue(new ByteArrayInputStream("Apple".getBytes()), DataType.INPUT_STREAM);
    TypedValue repeatableTypedValue = (TypedValue) streamingHelper.resolveCursorProvider(typedValue);

    assertThat(repeatableTypedValue.getValue(), instanceOf(CursorProvider.class));
  }

  @Test
  public void resolveNonStreamableTypedValueProvider() {
    TypedValue typedValue = new TypedValue("Apple", DataType.STRING);
    TypedValue repeatableTypedValue = (TypedValue) streamingHelper.resolveCursorProvider(typedValue);

    assertThat(repeatableTypedValue.getValue(), not(instanceOf(CursorProvider.class)));
  }

}
