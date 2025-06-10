/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.BYTES_STREAMING;

import static java.lang.Thread.currentThread;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(BYTES_STREAMING)
public class InputStreamBufferTestCase extends AbstractMuleTestCase {

  private static final int ACTUAL_STREAM_BUFFER_SIZE = 8;

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void streamDataAvailableEventually() throws IOException {
    final InputStream is = new InputStream() {

      private boolean firstRead = false;

      @Override
      public int read() throws IOException {
        return 0;
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        if (!firstRead) {
          firstRead = true;
          return 0;
        }

        return super.read(b, off, len);
      }

      @Override
      public int available() throws IOException {
        return 0;
      }
    };

    final InMemoryStreamBuffer streamBuffer =
        new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    final ByteBuffer byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE);

    assertThat(byteBuffer.limit(), is(ACTUAL_STREAM_BUFFER_SIZE));
    assertThat(byteBuffer.capacity(), is(ACTUAL_STREAM_BUFFER_SIZE));
  }

  @Test
  public void streamFinished() throws IOException {
    final InputStream is = new InputStream() {

      @Override
      public int read() throws IOException {
        return -1;
      }

      @Override
      public int available() throws IOException {
        return 0;
      }
    };

    final InMemoryStreamBuffer streamBuffer =
        new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    final ByteBuffer byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE);

    assertThat(byteBuffer, nullValue());
  }

  @Test
  public void streamFinishedAfterSomeData() throws IOException {
    final InputStream is = new InputStream() {

      private long bytesRead;

      @Override
      public int read() throws IOException {
        if (bytesRead > ACTUAL_STREAM_BUFFER_SIZE) {
          return -1;
        }

        bytesRead++;
        return 0;
      }

      @Override
      public int available() throws IOException {
        return ACTUAL_STREAM_BUFFER_SIZE;
      }
    };

    final InMemoryStreamBuffer streamBuffer =
        new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    final ByteBuffer byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE);

    assertThat(byteBuffer.limit(), is(ACTUAL_STREAM_BUFFER_SIZE));
    assertThat(byteBuffer.capacity(), is(ACTUAL_STREAM_BUFFER_SIZE));
  }

  @Test
  @Issue("W-18716253")
  public void streamAvailableDataSmallerThanAvaiableBuffer() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);

    final InputStream is = new InputStream() {

      private long bytesRead;

      @Override
      public int read() throws IOException {
        if (bytesRead > 0 && bytesRead % ACTUAL_STREAM_BUFFER_SIZE == 0) {
          try {
            latch.await();
          } catch (InterruptedException e) {
            currentThread().interrupt();
            throw new IOException(e);
          }
        }

        bytesRead++;
        return 0;
      }

      @Override
      public int available() throws IOException {
        return (int) (ACTUAL_STREAM_BUFFER_SIZE - bytesRead % ACTUAL_STREAM_BUFFER_SIZE);
      }
    };

    final InMemoryStreamBuffer streamBuffer =
        new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    // request one more than the available buffer
    final ByteBuffer byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE + 1);

    assertThat(byteBuffer.limit(), is(ACTUAL_STREAM_BUFFER_SIZE));
    assertThat(byteBuffer.capacity(), is(ACTUAL_STREAM_BUFFER_SIZE));
  }

  @Test
  public void streamIoException() throws IOException {
    final InputStream is = new InputStream() {

      @Override
      public int read() throws IOException {
        throw new IOException("Expected");
      }

    };

    final InMemoryStreamBuffer streamBuffer =
        new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());

    expectedException.expect(IOException.class);
    expectedException.expectMessage(is("Expected"));
    streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE);
    assertThat(currentThread().isInterrupted(), is(false));
  }

  @Test
  public void streamReadInterrupted() throws IOException {
    final InputStream is = new InputStream() {

      @Override
      public int read() throws IOException {
        currentThread().interrupt();
        throw new IOException("Expected");
      }

    };

    final InMemoryStreamBuffer streamBuffer =
        new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    final ByteBuffer byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE);
    assertThat(byteBuffer, is(nullValue()));
  }

}
