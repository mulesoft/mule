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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.exception.StreamingBufferSizeExceededException;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.internal.streaming.object.factory.InMemoryCursorIteratorProviderFactory;
import org.mule.runtime.core.internal.streaming.object.factory.NullCursorIteratorProviderFactory;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

@SmallTest
public class DefaultStreamingHelperTestCase extends AbstractMuleContextTestCase {

  private StreamingHelper streamingHelper;
  private StreamingManager streamingManager;
  private CoreEvent event;

  private static int CONTENT_CHARACTER = 140;
  protected static InputStream INFINITE_INPUT_STREAM = new InputStream() {

    @Override
    public int read() throws IOException {
      return CONTENT_CHARACTER;
    }
  };
  private static int DEFAULT_MAX_IN_MEMORY_STREAM_CAPACITY = InMemoryCursorStreamConfig.getDefault().getMaxBufferSize().toBytes();

  private List<String> valueList = Arrays.asList("Apple", "Banana", "Kiwi");

  @Override
  protected void doSetUp() throws Exception {
    streamingManager = new DefaultStreamingManager();
    initialiseIfNeeded(streamingManager, true, muleContext);
    setUpWithCursorProviderFactory(new InMemoryCursorIteratorProviderFactory(InMemoryCursorIteratorConfig.getDefault(),
                                                                             streamingManager));
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

  @Test
  public void resolveInputStreamFromInMemoryIteratorCursorFactoryStreamingHelper() throws Exception {
    setUpWithCursorProviderFactory(new InMemoryCursorIteratorProviderFactory(InMemoryCursorIteratorConfig.getDefault(),
                                                                             streamingManager));
    InputStream inputStream = new ByteArrayInputStream("Apple".getBytes());
    Object repeatableInputStream = streamingHelper.resolveCursorProvider(inputStream);
    assertThat(repeatableInputStream, instanceOf(CursorProvider.class));
  }

  @Test
  public void resolveInputStreamFromNullIteratorCursorFactoryStreamingHelper() throws Exception {
    setUpWithCursorProviderFactory(new NullCursorIteratorProviderFactory(streamingManager));
    InputStream inputStream = new ByteArrayInputStream("Apple".getBytes());
    Object repeatableInputStream = streamingHelper.resolveCursorProvider(inputStream);
    assertThat(repeatableInputStream, not(instanceOf(CursorProvider.class)));
  }

  @Test
  public void inMemoryStreamThatDoesNotFitInMemoryLimit() throws Exception {
    setUpWithCursorProviderFactory(new InMemoryCursorIteratorProviderFactory(InMemoryCursorIteratorConfig.getDefault(),
                                                                             streamingManager));
    InputStream repeatableInputStream =
        ((CursorStreamProvider) streamingHelper.resolveCursorProvider(INFINITE_INPUT_STREAM)).openCursor();
    boolean bufferExceeded = false;
    try {
      for (int i = 0; i < DEFAULT_MAX_IN_MEMORY_STREAM_CAPACITY + 1; i++) {
        repeatableInputStream.read();
      }
    } catch (StreamingBufferSizeExceededException streamingBufferSizeExceededException) {
      bufferExceeded = true;
    }
    assertTrue(bufferExceeded);
  }

  @Test
  public void nonRepeatableThatDoesNotFitInMemoryLimit() throws Exception {
    setUpWithCursorProviderFactory(new NullCursorIteratorProviderFactory(streamingManager));
    InputStream repeatableInputStream = (InputStream) streamingHelper.resolveCursorProvider(INFINITE_INPUT_STREAM);
    boolean readFailed = false;
    try {
      for (int i = 0; i < DEFAULT_MAX_IN_MEMORY_STREAM_CAPACITY + 1; i++) {
        repeatableInputStream.read();
      }
    } catch (Exception exception) {
      readFailed = true;
    }
    assertFalse(readFailed);
  }

  protected void setUpWithCursorProviderFactory(CursorProviderFactory cursorProviderFactory) throws Exception {
    cursorProviderFactory = cursorProviderFactory;
    event = testEvent();
    streamingHelper = new DefaultStreamingHelper(cursorProviderFactory, streamingManager, event);
  }

}
