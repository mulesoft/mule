/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.Channels.newChannel;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.DataUnit.BYTE;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.core.streaming.bytes.FileStoreCursorStreamConfig;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Test;

@SmallTest
public class FileStoreInputStreamBufferTestCase extends AbstractByteStreamingTestCase {

  private final int bufferSize = KB_256;
  private final ScheduledExecutorService executorService = newSingleThreadScheduledExecutor();

  private FileStoreInputStreamBuffer buffer;
  private ByteBufferManager bufferManager = new PoolingByteBufferManager();

  public FileStoreInputStreamBufferTestCase() {
    super(MB_2);
    FileStoreCursorStreamConfig config = new FileStoreCursorStreamConfig(new DataSize(bufferSize, BYTE));
    InputStream stream = new ByteArrayInputStream(data.getBytes());
    buffer = new FileStoreInputStreamBuffer(stream, newChannel(stream), config, null, bufferManager, executorService);
  }

  @After
  public void after() {
    try {
      if (buffer != null) {
        buffer.close();
      }
    } finally {
      executorService.shutdownNow();
    }
  }

  @Test
  public void getSliceOfCurrentBufferSegment() throws Exception {
    final int position = bufferSize / 4;
    int len = (bufferSize / 2) - position;
    ByteBuffer dest = allocate(len);

    assertThat(buffer.get(dest, position, len), is(len));
    assertThat(toString(dest.array()), equalTo(data.substring(position, position + len)));
  }

  @Test
  public void getSliceWhichStartsInCurrentSegmentButEndsInTheNext() throws Exception {
    final int position = bufferSize - 10;
    final int len = bufferSize / 2;
    ByteBuffer dest = allocate(len);

    int totalRead = 0;
    int read;
    int readPosition = position;
    int remainingLen = len;

    do {
      read = buffer.get(dest, readPosition, remainingLen);
      if (read > 0) {
        totalRead += read;
        readPosition += read;
        remainingLen -= read;
      }
    } while (read > 0);

    assertThat(totalRead, is(len));
    assertThat(toString(dest.array()), equalTo(data.substring(position, position + len)));
  }
}
