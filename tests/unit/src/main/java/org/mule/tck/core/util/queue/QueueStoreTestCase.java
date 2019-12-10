/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.util.queue;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.util.queue.QueueConfiguration.MAXIMUM_CAPACITY;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.util.queue.QueueStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

import java.io.Serializable;
import java.util.ArrayList;

public abstract class QueueStoreTestCase extends AbstractMuleContextTestCase {

  public static final String VALUE = "value";
  public static final String ANOTHER_VALUE = "value2";
  public static final long OFFER_TIMEOUT = 10l;
  public static final int NUMBER_OF_ITEMS = 10;
  public static final int LONG_POLL_TIMEOUT = 100;
  public static final int SHORT_POLL_TIMEOUT = 100;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void offerAndPollSingleValue() throws InterruptedException, ObjectStoreException {
    QueueStore queue = createQueue();
    queue.offer(VALUE, 0, OFFER_TIMEOUT);
    Serializable result = queue.poll(OFFER_TIMEOUT);
    assertThat((String) result, is(VALUE));
  }

  @Test
  public void offerAndPollTwice() throws Exception {
    QueueStore queue = createQueue();
    queue.putNow(VALUE);
    Serializable value = queue.poll(SHORT_POLL_TIMEOUT);
    assertThat((String) value, is(VALUE));
    assertThat(queue.poll(SHORT_POLL_TIMEOUT), nullValue());
  }

  @Test
  public void failIfThereIsNoCapacity() throws Exception {
    QueueStore queue = createQueueWithCapacity(1);
    assertThat(queue.offer(VALUE, 0, 10), is(true));
    assertThat(queue.offer(VALUE, 0, 10), is(false));
  }

  @Test
  public void allowOfferWhenThereIsCapacity() throws Exception {
    QueueStore queue = createQueueWithCapacity(1);
    assertThat(queue.offer(VALUE, 0, 10), is(true));
    queue.poll(10);
    assertThat(queue.offer(VALUE, 0, 10), is(true));
  }

  @Test
  public void untakeAddsElementFirst() throws Exception {
    QueueStore queue = createQueue();
    queue.offer(VALUE, 0, 10);
    queue.untake(ANOTHER_VALUE);
    assertThat((String) queue.poll(10), is(ANOTHER_VALUE));
  }

  @Test
  public void clearEmptiesTheQueue() throws Exception {
    QueueStore queue = createQueue();
    queue.putNow(VALUE);
    queue.putNow(ANOTHER_VALUE);
    queue.clear();
    assertThat(queue.poll(SHORT_POLL_TIMEOUT), nullValue());
  }

  @Test
  public void pollDoesNotReturnsUntilPollTimeout() throws Exception {
    QueueStore queue = createQueue();
    long initialTime = System.currentTimeMillis();
    queue.poll(LONG_POLL_TIMEOUT);
    assertThat(System.currentTimeMillis() - initialTime >= LONG_POLL_TIMEOUT, is(true));
  }

  @Test
  public void peekDoesNotRemoveElement() throws Exception {
    QueueStore queue = createQueue();
    queue.putNow(VALUE);
    assertThat((String) queue.peek(), Is.is(VALUE));
    assertThat((String) queue.poll(SHORT_POLL_TIMEOUT), Is.is(VALUE));
  }

  @Test
  public void offerSeveralRetrieveAll() throws Exception {
    QueueStore queue = createQueue();
    for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
      queue.offer(String.valueOf(i), 0, NUMBER_OF_ITEMS);
    }
    for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
      assertThat((String) queue.poll(NUMBER_OF_ITEMS), is(String.valueOf(i)));
    }
  }

  @Test
  public void offerSeveralRetrieveAllMuleEvents() throws Exception {
    QueueStore queue = createQueue();
    ArrayList<CoreEvent> events = new ArrayList<>(NUMBER_OF_ITEMS);
    for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
      CoreEvent testEvent = eventBuilder(muleContext).message(of("some data")).build();
      events.add(testEvent);
      queue.offer(testEvent, 0, NUMBER_OF_ITEMS);

    }
    for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
      assertThat(((CoreEvent) queue.poll(NUMBER_OF_ITEMS)).getContext().getId().equals(events.get(i).getContext().getId()),
                 is(true));
    }
  }

  protected QueueStore createQueue() {
    return createQueueWithCapacity(MAXIMUM_CAPACITY);
  }

  protected QueueStore createQueueWithCapacity(int capacity) {
    MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    when(mockMuleContext.getConfiguration().getWorkingDirectory()).thenReturn(temporaryFolder.getRoot().getAbsolutePath());
    when(mockMuleContext.getExecutionClassLoader()).thenReturn(muleContext.getExecutionClassLoader());
    when(mockMuleContext.getObjectSerializer()).thenReturn(muleContext.getObjectSerializer());
    QueueStore queue = createQueueInfoDelegate(capacity, mockMuleContext);
    return queue;
  }

  protected abstract QueueStore createQueueInfoDelegate(int capacity, MuleContext mockMuleContext);
}
