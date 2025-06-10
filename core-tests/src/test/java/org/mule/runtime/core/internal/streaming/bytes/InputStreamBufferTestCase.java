/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(BYTES_STREAMING)
class InputStreamBufferTestCase extends AbstractMuleTestCase {

  private static final int ACTUAL_STREAM_BUFFER_SIZE = 8;

  @Test
  void streamDataAvailableEventually() throws IOException {
    final var is = new InputStream() {

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

    final var streamBuffer = new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    final var byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE);

    assertThat(byteBuffer.limit(), is(ACTUAL_STREAM_BUFFER_SIZE));
    assertThat(byteBuffer.capacity(), is(ACTUAL_STREAM_BUFFER_SIZE));
  }

  @Test
  void streamFinished() throws IOException {
    final var is = new InputStream() {

      @Override
      public int read() throws IOException {
        return -1;
      }

      @Override
      public int available() throws IOException {
        return 0;
      }
    };

    final var streamBuffer = new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    final var byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE);

    assertThat(byteBuffer, nullValue());
  }

  @Test
  void streamFinishedAfterSomeData() throws IOException {
    final var is = new InputStream() {

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

    final var streamBuffer = new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    final var byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE);

    assertThat(byteBuffer.limit(), is(ACTUAL_STREAM_BUFFER_SIZE));
    assertThat(byteBuffer.capacity(), is(ACTUAL_STREAM_BUFFER_SIZE));
  }

  @Test
  @Issue("W-18716253")
  void streamAvailableDataSmallerThanAvaiableBuffer() throws IOException {
    final var latch = new CountDownLatch(1);

    final var is = new InputStream() {

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

    final var streamBuffer = new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    // request one more than the available buffer
    final var byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE + 1);

    assertThat(byteBuffer.limit(), is(ACTUAL_STREAM_BUFFER_SIZE));
    assertThat(byteBuffer.capacity(), is(ACTUAL_STREAM_BUFFER_SIZE));
  }

  @Test
  void streamIoException() {
    final var is = new InputStream() {

      @Override
      public int read() throws IOException {
        throw new IOException("Expected");
      }

    };

    final var streamBuffer = new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    var thrown = assertThrows(IOException.class, () -> streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE));
    assertThat(thrown.getMessage(), is("Expected"));
    assertThat(currentThread().isInterrupted(), is(false));
  }

  @Test
  void streamReadInterrupted() throws IOException {
    final var is = new InputStream() {

      @Override
      public int read() throws IOException {
        currentThread().interrupt();
        throw new IOException("Expected");
      }

    };

    final var streamBuffer = new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    final var byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE);
    assertThat(byteBuffer, is(nullValue()));
  }

}
