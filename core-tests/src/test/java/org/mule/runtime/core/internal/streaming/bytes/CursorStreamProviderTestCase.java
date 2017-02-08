/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.toIntExact;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.util.DataUnit.BYTE;
import org.mule.runtime.api.streaming.CursorStream;
import org.mule.runtime.api.streaming.CursorStreamProvider;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.streaming.bytes.FileStoreCursorStreamConfig;
import org.mule.runtime.core.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.util.func.CheckedConsumer;
import org.mule.runtime.core.util.func.CheckedRunnable;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@SmallTest
public class CursorStreamProviderTestCase extends AbstractByteStreamingTestCase {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"In Memory Without expansion", KB_256, MB_1, MB_1},
        {"In Memory With expansion", KB_256, MB_1, MB_2},
        {"File Store", MB_2, KB_256, KB_256}
    });
  }

  private final int halfDataLength;
  private final int bufferSize;
  private final ScheduledExecutorService executorService;

  private CursorStreamProvider streamProvider;
  private CountDownLatch controlLatch;
  private CountDownLatch mainThreadLatch;

  public CursorStreamProviderTestCase(String name, int dataSize, int bufferSize, int maxBufferSize) {
    super(dataSize);
    executorService = newScheduledThreadPool(2);
    this.bufferSize = bufferSize;
    halfDataLength = data.length() / 2;
    final ByteArrayInputStream dataStream = new ByteArrayInputStream(data.getBytes());

    if (dataSize <= bufferSize) {
      InMemoryCursorStreamConfig config =
          new InMemoryCursorStreamConfig(new DataSize(bufferSize, BYTE),
                                         new DataSize(bufferSize / 2, BYTE),
                                         new DataSize(maxBufferSize, BYTE));

      streamProvider = new InMemoryCursorStreamProvider(dataStream, config, mock(Event.class));
    } else {
      streamProvider = new FileStoreCursorStreamProvider(dataStream,
                                                         new FileStoreCursorStreamConfig(new DataSize(maxBufferSize, BYTE)),
                                                         mock(Event.class),
                                                         executorService);
    }

    resetLatches();
  }

  @After
  public void after() {
    streamProvider.close();
    executorService.shutdownNow();
  }

  @Test
  public void readFullyWithInSingleCursor() throws IOException {
    withCursor(cursor -> assertThat(IOUtils.toString(cursor), equalTo(data)));
  }

  @Test
  public void readFullyByteByByteWithSingleCursor() throws IOException {
    withCursor(cursor -> {
      for (int i = 0; i < data.length(); i++) {
        assertThat((char) cursor.read(), equalTo(data.charAt(i)));
      }
    });
  }

  @Test
  public void partialReadOnSingleCursor() throws Exception {
    byte[] dest = new byte[halfDataLength];

    withCursor(cursor -> {
      cursor.read(dest, 0, halfDataLength);
      assertThat(toString(dest), equalTo(data.substring(0, halfDataLength)));
    });
  }

  @Test
  public void partialReadWithOffsetOnSingleCursor() throws Exception {
    byte[] dest = new byte[halfDataLength + 2];

    dest[0] = "!".getBytes()[0];
    dest[1] = dest[0];

    withCursor(cursor -> {
      cursor.read(dest, 2, halfDataLength);
      assertThat(toString(dest), equalTo("!!" + data.substring(0, halfDataLength)));
    });
  }

  @Test
  public void randomSeekWithOneOpenCursor() throws Exception {
    withCursor(cursor -> {
      // read fully
      assertThat(IOUtils.toString(cursor), equalTo(data));

      System.out.println(data);
      // go back and read first 10 bytes
      seekAndAssert(cursor, 0, 10);

      // move to the middle and read the rest
      seekAndAssert(cursor, halfDataLength, halfDataLength);
    });
  }

  @Test
  public void twoOpenCursorsConsumingTheStreamInSingleThread() throws Exception {
    withCursor(cursor1 -> withCursor(cursor2 -> {
      seekAndAssert(cursor1, 0, data.length());
      seekAndAssert(cursor2, 0, data.length());
    }));
  }

  @Test
  public void twoOpenCursorsReadingOppositeEndsOfTheStreamInSingleThread() throws Exception {
    withCursor(cursor1 -> withCursor(cursor2 -> {
      seekAndAssert(cursor1, 0, data.length() / 2);
      seekAndAssert(cursor2, halfDataLength, halfDataLength);
    }));
  }

  @Test
  public void twoOpenCursorsConsumingTheStreamConcurrently() throws Exception {
    withCursor(cursor1 -> withCursor(cursor2 -> doAsync(() -> seekAndAssert(cursor1, 0, data.length()),
                                                        () -> seekAndAssert(cursor2, 0, data.length()))));
  }

  @Test
  public void twoOpenCursorsReadingOppositeEndsOfTheStreamConcurrently() throws Exception {
    withCursor(cursor1 -> withCursor(cursor2 -> doAsync(() -> seekAndAssert(cursor1, 0, data.length() / 2),
                                                        () -> seekAndAssert(cursor2, halfDataLength, halfDataLength))));
  }

  @Test
  public void getPosition() throws Exception {
    withCursor(cursor -> {
      assertThat(cursor.getPosition(), is(0L));

      cursor.seek(10);
      assertThat(cursor.getPosition(), is(10L));

      cursor.seek(0);
      assertThat(cursor.getPosition(), is(0L));

    });
  }

  @Test
  public void isClosed() throws Exception {
    withCursor(cursor -> {
      assertThat(cursor.isClosed(), is(false));
      IOUtils.toString(cursor);
      assertThat(cursor.isClosed(), is(true));
      cursor.seek(0);
      assertThat(cursor.isClosed(), is(false));
    });
  }

  @Test
  public void getSliceWhichStartsInCurrentSegmentButEndsInTheNext() throws Exception {
    if (data.length() < bufferSize) {
      // this test only makes sense for off heap streams
      return;
    }

    final int position = bufferSize - 10;
    final int len = bufferSize / 2;
    byte[] dest = new byte[len];

    withCursor(cursor -> {
      cursor.seek(position);
      assertThat(cursor.read(dest, 0, len), is(len));
      assertThat(toString(dest), equalTo(data.substring(position, position + len)));
    });
  }

  private void doAsync(CheckedRunnable task1, CheckedRunnable task2) throws Exception {
    resetLatches();
    Future future1 = doAsync(() -> {
      controlLatch.await();
      task1.run();
      mainThreadLatch.countDown();
    });

    Future future2 = doAsync(() -> {
      controlLatch.countDown();
      task2.run();
      mainThreadLatch.countDown();
    });

    awaitMainThreadLatch();
    assertThat(future1.get(), is(nullValue()));
    assertThat(future2.get(), is(nullValue()));
  }

  private Future doAsync(CheckedRunnable task) {
    return executorService.submit(() -> {
      try {
        task.run();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void awaitMainThreadLatch() throws InterruptedException {
    mainThreadLatch.await(1, SECONDS);
  }


  private void seekAndAssert(CursorStream cursor, long position, int length) throws Exception {
    byte[] randomBytes = new byte[length];
    cursor.seek(position);
    cursor.read(randomBytes, 0, length);
    assertThat(toString(randomBytes), equalTo(data.substring(toIntExact(position), toIntExact(position + length))));
  }

  private void resetLatches() {
    controlLatch = new CountDownLatch(1);
    mainThreadLatch = new CountDownLatch(2);
  }

  private void withCursor(CheckedConsumer<CursorStream> consumer) throws IOException {
    try (CursorStream cursor = streamProvider.openCursor()) {
      consumer.accept(cursor);
    }
  }
}
