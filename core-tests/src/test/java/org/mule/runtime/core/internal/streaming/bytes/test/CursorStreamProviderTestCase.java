/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes.test;

import static org.mule.runtime.api.util.DataUnit.BYTE;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.DEFAULT_BUFFER_BUCKET_SIZE;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;

import static java.lang.Math.min;
import static java.lang.Math.toIntExact;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.apache.commons.lang3.RandomStringUtils.insecure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamProvider;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.streaming.bytes.PoolingByteBufferManager;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@RunWith(Parameterized.class)
@SmallTest
@Feature(STREAMING)
public class CursorStreamProviderTestCase extends AbstractByteStreamingTestCase {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"Doesn't require expansion, eagerRead", KB_256, MB_1, MB_2, true},
        {"Doesn't require expansion, !eagerRead", KB_256, MB_1, MB_2, false},
        {"Requires expansion, eagerRead", MB_1, KB_256, MB_2, true},
        {"Requires expansion, !eagerRead", MB_1, KB_256, MB_2, false},
    });
  }

  private int halfDataLength;
  private final int bufferSize;
  private final int maxBufferSize;
  protected final ScheduledExecutorService executorService;

  private CursorStreamProvider streamProvider;
  private CountDownLatch controlLatch;
  private CountDownLatch mainThreadLatch;
  private PoolingByteBufferManager bufferManager;
  private boolean eagerRead;

  public CursorStreamProviderTestCase(String name, int dataSize, int bufferSize, int maxBufferSize, boolean eagerRead) {
    super(dataSize);
    executorService = newScheduledThreadPool(2);
    this.bufferSize = bufferSize;
    this.maxBufferSize = maxBufferSize;
    halfDataLength = data.length() / 2;
    this.eagerRead = eagerRead;

    resetLatches();
  }

  @Before
  public void before() {
    bufferManager = new PoolingByteBufferManager();
    final InputStream dataStream = createDataStream();
    streamProvider = createStreamProvider(bufferSize, maxBufferSize, dataStream, eagerRead);
  }

  protected InputStream createDataStream() {
    return new ByteArrayInputStream(data.getBytes());
  }

  protected CursorStreamProvider createStreamProvider(int bufferSize, int maxBufferSize, InputStream dataStream,
                                                      boolean eagerRead) {
    InMemoryCursorStreamConfig config =
        new InMemoryCursorStreamConfig(new DataSize(bufferSize, BYTE),
                                       new DataSize(bufferSize / 2, BYTE),
                                       new DataSize(maxBufferSize, BYTE),
                                       eagerRead);

    return new InMemoryCursorStreamProvider(dataStream, config, bufferManager, from("log"), false);
  }

  @After
  public void after() {
    streamProvider.close();
    executorService.shutdownNow();
    bufferManager.dispose();
  }

  @Test
  public void readFullyWithInSingleCursor() throws Exception {
    withCursor(cursor -> assertEquals(IOUtils.toString(cursor, UTF_8), data));
  }

  @Test
  public void readFullyByteByByteWithSingleCursor() throws Exception {
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
      readCursor(cursor, dest, 0, halfDataLength);
      assertEquals(toString(dest), data.substring(0, halfDataLength));
    });
  }

  @Test
  public void rewindWhileStreamNotFullyConsumed() throws Exception {
    withCursor(cursor -> {
      byte[] dest = new byte[halfDataLength];
      readCursor(cursor, dest, 0, halfDataLength);
      assertEquals(toString(dest), data.substring(0, halfDataLength));
      cursor.seek(0);
      dest = new byte[data.length()];

      readCursor(cursor, dest, 0, dest.length);
      assertEquals(toString(dest), data);
    });

  }

  @Test
  public void partialReadWithOffsetOnSingleCursor() throws Exception {
    byte[] dest = new byte[halfDataLength + 2];

    dest[0] = "!".getBytes()[0];
    dest[1] = dest[0];

    withCursor(cursor -> {
      readCursor(cursor, dest, 2, halfDataLength);
      assertEquals(toString(dest), "!!" + data.substring(0, halfDataLength));
    });
  }

  @Test
  public void randomSeekWithOneOpenCursor() throws Exception {
    withCursor(cursor -> {
      // read fully
      assertEquals(IOUtils.toString(cursor, UTF_8), data);

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
    withCursor(cursor1 -> withCursor(cursor2 -> {
      seekAndAssert(cursor1, 0, halfDataLength);
      seekAndAssert(cursor2, halfDataLength, halfDataLength);
    }));
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
  public void consumeByChunksShorterThanBufferSize() throws Exception {
    withCursor(cursor -> assertEquals(readByChunks(cursor, bufferSize / 2), data));
  }

  @Test
  public void readsMostOfTheStreamInFirstAccessAndRemainderInSecond() throws Exception {
    byte[] dest = new byte[data.length()];
    halfDataLength = Double.valueOf(Math.floor(halfDataLength * .8)).intValue();
    withCursor(cursor -> {
      int read = readCursor(cursor, dest, 0, halfDataLength);
      assertThat(read, is(halfDataLength));
      assertEquals(toString(dest, 0, halfDataLength), data.substring(0, halfDataLength));

      int secondRead = readCursor(cursor, dest, read, data.length() - read);

      assertThat(secondRead, is(data.length() - read));
      assertEquals(toString(dest, halfDataLength, data.length() - read), data.substring(halfDataLength));
    });
  }

  @Test
  public void consumeByChunksWhichOverlapWithBuffer() throws Exception {
    StringBuilder accumulator = new StringBuilder();
    withCursor(cursor -> {

      // read a chunk which is significantly smaller than the buffer size
      int chunkSize = bufferSize / 2;
      byte[] buffer = new byte[chunkSize];
      int read = cursor.read(buffer, 0, chunkSize);
      append(buffer, read, accumulator);

      // read the rest at bigger chunk rates
      accumulator.append(readByChunks(cursor, bufferSize));
    });

    assertEquals(accumulator.toString(), data);
  }

  @Test
  public void getSliceWhichStartsBehindInCurrentSegmentButEndsInTheCurrent() throws Exception {
    if (data.length() < bufferSize) {
      // this test only makes sense for larger than memory streams
      return;
    }

    int len = bufferSize + 20;
    byte[] dest = new byte[len];

    withCursor(cursor -> {
      assertThat(readCursor(cursor, dest, 0, len), is(len));
      assertEquals(toString(dest), data.substring(0, len));

      final int position = bufferSize - 30;
      cursor.seek(position);
      assertThat(readCursor(cursor, dest, 0, len), is(len));

      assertEquals(toString(dest), data.substring(position, position + len));
    });
  }

  @Test
  public void getSliceWhichStartsInCurrentSegmentButEndsInTheNext() throws Exception {
    if (data.length() < bufferSize) {
      // this test only makes sense for larger than memory streams
      return;
    }

    final int position = bufferSize - 10;
    final int len = bufferSize / 2;
    byte[] dest = new byte[len];

    withCursor(cursor -> {
      cursor.seek(position);
      assertThat(readCursor(cursor, dest, 0, len), is(len));
      assertEquals(toString(dest), data.substring(position, position + len));
    });
  }

  @Test
  public void dataLengthMatchesMaxBufferSizeExactly() throws Exception {
    data = insecure().nextAlphanumeric(maxBufferSize);
    final InputStream dataStream = createDataStream();

    InMemoryCursorStreamConfig config =
        new InMemoryCursorStreamConfig(new DataSize(maxBufferSize, BYTE),
                                       new DataSize(0, BYTE),
                                       new DataSize(maxBufferSize, BYTE),
                                       eagerRead);

    streamProvider = new InMemoryCursorStreamProvider(dataStream, config, bufferManager, null, false);
    withCursor(cursor -> assertEquals(IOUtils.toString(cursor, UTF_8), data));
  }

  @Test
  public void mark() throws Exception {
    withCursor(cursor -> {
      final int mark = 10;
      assertThat(cursor.read(new byte[mark], 0, mark), is(mark));

      final long position = cursor.getPosition();
      cursor.mark(100);
      assertThat(cursor.read(new byte[100], 0, 100), is(100));

      cursor.reset();
      assertEquals(toString(cursor), data.substring(toIntExact(position)));
    });
  }

  @Test
  public void ioExceptionIfClosed() throws Exception {
    CursorStream cursor = streamProvider.openCursor();
    cursor.close();
    assertThrows(IOException.class, () -> cursor.read());
  }

  @Test
  @Issue("W-18716253")
  public void eagerReadLimit() throws IOException {
    CursorStream cursor = streamProvider.openCursor();

    final var read = cursor.read(new byte[MB_2], 0, MB_2);

    if (eagerRead) {
      assertThat(read, is(min(DEFAULT_BUFFER_BUCKET_SIZE, maxBufferSize)));
    } else {
      assertThat(read, greaterThanOrEqualTo(KB_256));
    }
  }

  private void doAsync(CheckedRunnable task1, CheckedRunnable task2) throws Exception {
    resetLatches();
    Future<?> future1 = doAsync(() -> {
      controlLatch.await();
      task1.run();
      mainThreadLatch.countDown();
    });

    Future<?> future2 = doAsync(() -> {
      controlLatch.countDown();
      task2.run();
      mainThreadLatch.countDown();
    });

    awaitMainThreadLatch();
    assertThat(future1.get(), is(nullValue()));
    assertThat(future2.get(), is(nullValue()));
  }

  private Future<?> doAsync(CheckedRunnable task) {
    return executorService.submit(() -> {
      try {
        task.run();
      } catch (Exception e) {
        throw new RuntimeException(unwrap(e));
      }
    });
  }

  private void awaitMainThreadLatch() throws InterruptedException {
    mainThreadLatch.await(1, SECONDS);
  }


  private void seekAndAssert(CursorStream cursor, long position, int length) throws Exception {
    byte[] randomBytes = new byte[length];
    cursor.seek(position);
    readCursor(cursor, randomBytes, 0, length);
    assertEquals(toString(randomBytes), data.substring(toIntExact(position), toIntExact(position + length)));
  }

  private void resetLatches() {
    controlLatch = new CountDownLatch(1);
    mainThreadLatch = new CountDownLatch(2);
  }

  private void withCursor(CheckedConsumer<CursorStream> consumer) throws Exception {
    try (CursorStream cursor = streamProvider.openCursor()) {
      consumer.accept(cursor);
    } catch (Exception e) {
      throw new RuntimeException(unwrap(e));
    }
  }

  private String readByChunks(InputStream stream, int chunkSize) throws Exception {
    int read;
    StringBuilder accumulator = new StringBuilder();
    do {
      byte[] buffer = new byte[chunkSize];
      read = stream.read(buffer, 0, chunkSize);
      append(buffer, read, accumulator);
    } while (read > 0);

    return accumulator.toString();
  }

  private void append(byte[] buffer, int read, StringBuilder accumulator) {
    for (int i = 0; i < read; i++) {
      accumulator.append((char) buffer[i]);
    }
  }

  private int readCursor(CursorStream cursor, byte[] dest, int off, int len) throws IOException {
    if (eagerRead) {
      var r = 0;
      while (r < len) {
        r += cursor.read(dest, off + r, len - r);
      }
      return r;
    } else {
      return cursor.read(dest, off, len);
    }
  }

  public ByteBufferManager getBufferManager() {
    return bufferManager;
  }
}
