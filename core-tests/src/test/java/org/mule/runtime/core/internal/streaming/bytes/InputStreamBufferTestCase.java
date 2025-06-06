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
import static org.hamcrest.core.Is.is;

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
public class InputStreamBufferTestCase extends AbstractMuleTestCase {

  private static final int ACTUAL_STREAM_BUFFER_SIZE = 8;

  @Test
  @Issue("W-18716253")
  public void streamAvailableDataSmallerThanAvaiableBuffer() {
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
      public int read(byte[] b) throws IOException {
        return super.read(b);
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        return super.read(b, off, len);
      }

      @Override
      public int available() throws IOException {
        return (int) (ACTUAL_STREAM_BUFFER_SIZE - bytesRead % ACTUAL_STREAM_BUFFER_SIZE);
      }
    };

    final var streamBuffer = new InMemoryStreamBuffer(is, InMemoryCursorStreamConfig.getDefault(), new SimpleByteBufferManager());
    // request one more than the avaialble buffer
    final var byteBuffer = streamBuffer.get(0, ACTUAL_STREAM_BUFFER_SIZE + 1);

    assertThat(byteBuffer.limit(), is(ACTUAL_STREAM_BUFFER_SIZE));
  }
}
