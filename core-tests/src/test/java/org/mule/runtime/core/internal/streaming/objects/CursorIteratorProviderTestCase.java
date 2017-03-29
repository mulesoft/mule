/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.objects;

import static java.lang.Math.toIntExact;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.streaming.exception.StreamingBufferSizeExceededException;
import org.mule.runtime.api.streaming.objects.CursorIterator;
import org.mule.runtime.api.streaming.objects.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.object.InMemoryCursorIteratorProvider;
import org.mule.runtime.core.streaming.objects.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.util.func.CheckedConsumer;
import org.mule.runtime.core.util.func.CheckedRunnable;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@RunWith(Parameterized.class)
@SmallTest
@Features("Streaming")
@Stories("Object Streaming")
public class CursorIteratorProviderTestCase extends AbstractObjectStreamingTestCase {

  private static final int DATA_SIZE = 500;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"Doesn't require expansion", DATA_SIZE, new InMemoryCursorIteratorConfig(DATA_SIZE, 00, DATA_SIZE)},
        {"Requires expansion", DATA_SIZE, new InMemoryCursorIteratorConfig(100, 50, DATA_SIZE)},
    });
  }

  private final int halfDataLength;
  private final InMemoryCursorIteratorConfig config;
  protected final ScheduledExecutorService executorService;

  private CursorIteratorProvider streamProvider;
  private CountDownLatch controlLatch;
  private CountDownLatch mainThreadLatch;

  public CursorIteratorProviderTestCase(String name, int dataSize, InMemoryCursorIteratorConfig config) {
    super(dataSize);
    this.config = config;
    executorService = newScheduledThreadPool(2);
    halfDataLength = data.size() / 2;

    streamProvider = createStreamProvider(data);
    resetLatches();
  }

  protected CursorIteratorProvider createStreamProvider(List<String> data) {
    return new InMemoryCursorIteratorProvider(toStreamingIterator(data), config);
  }

  @After
  public void after() {
    streamProvider.close();
    executorService.shutdownNow();
  }

  @Test
  @Description("fully consume stream in a single thread")
  public void readFullyWithInSingleCursor() throws IOException {
    withCursor(cursor -> checkEquals(data, cursor));
  }

  @Test
  @Description("Partially consume the stream, rewind back to zero and consume fully")
  public void rewindWhileStreamNotFullyConsumed() throws Exception {
    withCursor(cursor -> {
      List<String> read = read(cursor, halfDataLength);
      checkEquals(read, data.subList(0, halfDataLength));

      cursor.seek(0);
      read = read(cursor, data.size());
      checkEquals(read, data);
    });
  }


  @Test
  @Description("Consume the stream, go back to two different positions and consume again (each)")
  public void randomSeekWithOneOpenCursor() throws Exception {
    withCursor(cursor -> {
      // read fully
      checkEquals(data, cursor);

      // go back and read first 10 items
      seekAndAssert(cursor, 0, 10);

      // move to the middle and read the rest
      seekAndAssert(cursor, halfDataLength, halfDataLength);
    });
  }

  @Test
  @Description("Two open cursors consume the same stream, one after the other in the same thread")
  public void twoOpenCursorsConsumingTheStreamInSingleThread() throws Exception {
    withCursor(cursor1 -> withCursor(cursor2 -> {
      seekAndAssert(cursor1, 0, data.size());
      seekAndAssert(cursor2, 0, data.size());
    }));
  }

  @Test
  @Description("Two open cursors consume different ends of the same stream, one after the other in the same thread")
  public void twoOpenCursorsReadingOppositeEndsOfTheStreamInSingleThread() throws Exception {
    withCursor(cursor1 -> withCursor(cursor2 -> {
      seekAndAssert(cursor1, 0, data.size() / 2);
      seekAndAssert(cursor2, halfDataLength, halfDataLength);
    }));
  }

  @Test
  @Description("Two open cursors consume the same stream concurrently, each on its own thread")
  public void twoOpenCursorsConsumingTheStreamConcurrently() throws Exception {
    withCursor(cursor1 -> withCursor(cursor2 -> doAsync(() -> seekAndAssert(cursor1, 0, data.size()),
                                                        () -> seekAndAssert(cursor2, 0, data.size()))));
  }

  @Test
  @Description("Two open cursors consume different ends of same stream concurrently, each on its own thread")
  public void twoOpenCursorsReadingOppositeEndsOfTheStreamConcurrently() throws Exception {
    withCursor(cursor1 -> withCursor(cursor2 -> doAsync(() -> seekAndAssert(cursor1, 0, data.size() / 2),
                                                        () -> seekAndAssert(cursor2, halfDataLength, halfDataLength))));
  }

  @Test
  @Description("Seek different positions and verify that getPosition() is consistent")
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
  @Description("Get the size of a stream")
  public void size() throws Exception {
    withCursor(cursor -> assertThat(cursor.size(), is(data.size())));
  }

  @Test(expected = StreamingBufferSizeExceededException.class)
  @Description("Exceed the maxBufferSize and expect exception")
  public void bufferSizeExceeded() throws Exception {
    data.add("I don't fit");
    streamProvider.close();
    streamProvider = createStreamProvider(data);

    withCursor(cursor -> read(cursor, data.size()));
  }

  @Test
  @Description("Direct access to the last two items of the stream without traversing the whole cursor")
  public void getLastTwoItems() throws Exception {
    withCursor(cursor -> {
      int size = data.size();
      cursor.seek(size - 2);

      assertThat(cursor.hasNext(), is(true));
      assertThat(cursor.next(), is(data.get(size - 2)));
      assertThat(cursor.next(), is(data.get(size - 1)));
      assertThat(cursor.hasNext(), is(false));
    });
  }

  @Test(expected = NoSuchElementException.class)
  @Description("Move the cursor to a non existing position")
  public void outOfBoundsHasNext() throws Exception {
    withCursor(cursor -> {
      cursor.seek(data.size() + 100);
      assertThat(cursor.hasNext(), is(false));
      cursor.next();
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


  private void seekAndAssert(CursorIterator<String> cursor, long position, int size) throws Exception {
    cursor.seek(position);
    List<String> read = read(cursor, size);
    checkEquals(read, data.subList(toIntExact(position), toIntExact(position + size)));
  }

  private void resetLatches() {
    controlLatch = new CountDownLatch(1);
    mainThreadLatch = new CountDownLatch(2);
  }

  private void withCursor(CheckedConsumer<CursorIterator> consumer) throws IOException {
    try (CursorIterator cursor = streamProvider.openCursor()) {
      consumer.accept(cursor);
    }
  }
}
