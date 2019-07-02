/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;
import org.mule.runtime.core.internal.util.journal.queue.LocalTxQueueTransactionJournal;
import org.mule.runtime.core.internal.util.journal.queue.LocalTxQueueTransactionRecoverer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.stubbing.Answer;

public class LocalTxQueueTransactionTestCase extends AbstractMuleContextTestCase {

  public static final String QUEUE_NAME = "inQueue";
  private static final int TIMEOUT = 10;

  private static final long QUEUE_DELAY_MILLIS = 2000;
  private DelayedQueueStore delayedQueue;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private LocalTxQueueTransactionJournal txLog;
  private DefaultQueueStore inQueue;
  private PersistentQueueTransactionContext persistentTransactionContext;
  private LocalTxQueueTransactionRecoverer queueTransactionRecoverer;
  private LocalTxQueueTransactionContext localTxTransactionContext;

  @Override
  protected void doSetUp() throws Exception {
    ((DefaultMuleConfiguration) muleContext.getConfiguration()).setWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
    txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);

    QueueConfiguration queueConfiguration = new DefaultQueueConfiguration(0, true);
    inQueue = new DefaultQueueStore(QUEUE_NAME, muleContext, queueConfiguration);
    delayedQueue = new DelayedQueueStore(QUEUE_NAME, muleContext, queueConfiguration, QUEUE_DELAY_MILLIS);

    persistentTransactionContext = new PersistentQueueTransactionContext(txLog, createRecoverOnlyQueueProvider(inQueue));
    queueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(txLog, createRecoverOnlyQueueProvider(inQueue));
    localTxTransactionContext = createLocalTxContext(txLog, createQueueProvider(delayedQueue));
  }

  private LocalTxQueueTransactionContext createLocalTxContext(LocalTxQueueTransactionJournal txLog, QueueProvider provider)
      throws InterruptedException {
    Lock lock = mock(Lock.class);
    when(lock.tryLock(anyLong(), eq(MILLISECONDS))).thenAnswer((Answer<Boolean>) invocationOnMock -> {
      sleep(invocationOnMock.getArgument(0, Long.class) / 2);
      return true;
    });
    return new LocalTxQueueTransactionContext(txLog, provider, lock);
  }

  @Override
  protected void configureMuleContext(MuleContextBuilder contextBuilder) {
    DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
    muleConfiguration.setWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
    contextBuilder.setMuleConfiguration(muleConfiguration);
  }

  @Test
  public void pollAndFailThenRecover() throws Exception {
    inQueue.offer(testEvent(), 0, TIMEOUT);
    Serializable value = persistentTransactionContext.poll(inQueue, 100000);
    assertThat(inQueue.poll(TIMEOUT), nullValue());
    assertThat(value, notNullValue());
    txLog.close();
    txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);

    queueTransactionRecoverer.recover();
    CoreEvent muleEvent = (CoreEvent) inQueue.poll(TIMEOUT);
    assertThat(muleEvent, notNullValue());
    assertThat(testEvent().getContext().getId(), equalTo(muleEvent.getContext().getId()));
  }

  @Test
  public void pollAndFailThenRecoverWithTwoElements() throws Exception {
    final String MESSAGE_CONTENT_2 = TEST_PAYLOAD + "2";
    CoreEvent testEvent2 = eventBuilder(muleContext).message(of(MESSAGE_CONTENT_2)).build();

    inQueue.offer(testEvent(), 0, TIMEOUT);
    inQueue.offer(testEvent2, 0, TIMEOUT);
    Serializable value = persistentTransactionContext.poll(inQueue, 100000);
    assertThat(inQueue.getSize(), is(1));
    assertThat(value, notNullValue());
    txLog.close();
    txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    queueTransactionRecoverer.recover();
    CoreEvent muleEvent = (CoreEvent) inQueue.poll(TIMEOUT);
    assertThat(muleEvent, notNullValue());
    assertThat(muleEvent.getMessage().getPayload().getValue().toString(), is(MESSAGE_CONTENT_2)); // recovered element should be
                                                                                                  // last

    muleEvent = (CoreEvent) inQueue.poll(TIMEOUT);
    assertThat(muleEvent, notNullValue());
    assertThat(muleEvent.getMessage().getPayload().getValue().toString(), is(TEST_PAYLOAD)); // recovered element
  }

  @Test
  public void failBetweenLogEntryWriteAndRealPoolThenRecover() throws Exception {
    inQueue.offer(testEvent(), 0, TIMEOUT);
    persistentTransactionContext.poll(inQueue, TIMEOUT);
    txLog.close();
    txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    queueTransactionRecoverer.recover();
    CoreEvent muleEvent = (CoreEvent) inQueue.poll(TIMEOUT);
    assertThat(muleEvent, notNullValue());
    assertThat(testEvent().getContext().getId(), equalTo(muleEvent.getContext().getId()));
    muleEvent = (CoreEvent) inQueue.poll(TIMEOUT);
    assertThat(muleEvent, nullValue());
  }

  private QueueProvider createRecoverOnlyQueueProvider(final DefaultQueueStore queue) {
    return new QueueProvider() {

      @Override
      public QueueStore getQueue(String queueName) {
        throw new NotImplementedException("This is test code");
      }

      @Override
      public RecoverableQueueStore getRecoveryQueue(String queueName) {
        return queue;
      }
    };
  }

  private QueueProvider createQueueProvider(final DefaultQueueStore queue) {
    return new QueueProvider() {

      @Override
      public QueueStore getQueue(String queueName) {
        return queue;
      }

      @Override
      public RecoverableQueueStore getRecoveryQueue(String queueName) {
        return queue;
      }
    };
  }

  @Test
  public void offerAndFailThenRecover() throws Exception {
    final DefaultQueueStore outQueue = new DefaultQueueStore(QUEUE_NAME, muleContext, new DefaultQueueConfiguration(0, true));
    persistentTransactionContext = new PersistentQueueTransactionContext(txLog, createRecoverOnlyQueueProvider(outQueue));
    persistentTransactionContext.offer(outQueue, testEvent(), TIMEOUT);
    assertThat(outQueue.poll(TIMEOUT), nullValue());
    txLog.close();
    txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    queueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(txLog, createRecoverOnlyQueueProvider(outQueue));
    queueTransactionRecoverer.recover();
    Serializable muleEvent = outQueue.poll(TIMEOUT);
    assertThat(muleEvent, nullValue());
  }

  @Test
  public void offerAndFailBetweenRealOfferAndCommitThenRecover() throws Exception {
    txLog = new TestTransactionLogger(temporaryFolder.getRoot().getAbsolutePath(), muleContext).failDuringLogCommit();
    final DefaultQueueStore outQueue = new DefaultQueueStore(QUEUE_NAME, muleContext, new DefaultQueueConfiguration(0, true));
    persistentTransactionContext = new PersistentQueueTransactionContext(txLog, createRecoverOnlyQueueProvider(outQueue));
    persistentTransactionContext.offer(outQueue, testEvent(), TIMEOUT);
    try {
      persistentTransactionContext.doCommit();
      fail();
    } catch (ResourceManagerException e) {
      // expected
    }
    txLog.close();
    txLog = new TestTransactionLogger(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    queueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(txLog, createRecoverOnlyQueueProvider(outQueue));
    queueTransactionRecoverer.recover();
    Serializable muleEvent = outQueue.poll(TIMEOUT);
    assertThat(muleEvent, nullValue());
  }

  @Test
  public void pollDoesNotReturnNullBeforeTimeoutUsingTransactionContext() throws InterruptedException {
    // When input queue is empty
    assertThat(inQueue.getSize(), is(0));

    long timeout = 300;

    // If we poll an element from the queue with a given timeout
    long timeBeforePoll = currentTimeMillis();
    Serializable polledValue = persistentTransactionContext.poll(inQueue, timeout);
    long timeAfterPoll = currentTimeMillis();

    // Then the value is null
    assertThat(polledValue, nullValue());
    // And it waited at least for the given timeout
    assertThat((double) (timeAfterPoll - timeBeforePoll),
               anyOf(greaterThanOrEqualTo((double) timeout), closeTo((double) timeout, 20)));
  }

  @Test
  public void pollDoesNotReturnNullBeforeTimeoutUsingDirectlyTheQueue() throws InterruptedException {
    // When input queue is empty
    assertThat(inQueue.getSize(), is(0));

    final long timeout = 300;

    // If we poll an element from the queue with a given timeout
    long timeBeforePoll = currentTimeMillis();
    Serializable polledValue = inQueue.poll(timeout);
    long timeAfterPoll = currentTimeMillis();

    // Then the value is null
    assertThat(polledValue, nullValue());
    // And it waited at least for the given timeout
    assertThat(timeAfterPoll - timeBeforePoll, greaterThanOrEqualTo(timeout));
  }

  @Test
  public void pollDoesNotDelayMuchMoreThanGivenTimeout() throws InterruptedException {
    long timeMillisBeforePoll = currentTimeMillis();
    localTxTransactionContext.poll(delayedQueue, QUEUE_DELAY_MILLIS);
    long elapsedMillis = currentTimeMillis() - timeMillisBeforePoll;

    final long toleranceMillis = 5;
    assertThat(elapsedMillis, lessThanOrEqualTo(QUEUE_DELAY_MILLIS + toleranceMillis));
  }

  @Test
  public void offerDoesNotDelayMuchMoreThanGivenTimeout() throws InterruptedException, ResourceManagerException {
    long waitTime = QUEUE_DELAY_MILLIS / 2;
    int item = 1;

    long timeMillisBeforeOffer = currentTimeMillis();
    localTxTransactionContext.offer(delayedQueue, item, waitTime);
    localTxTransactionContext.doCommit();
    long elapsedMillis = currentTimeMillis() - timeMillisBeforeOffer;

    final long toleranceMillis = 5;
    assertThat(elapsedMillis, lessThanOrEqualTo(waitTime + toleranceMillis));
  }

  private static class DelayedQueueStore extends DefaultQueueStore {

    private final long delayMillis;

    DelayedQueueStore(String name, MuleContext muleContext, QueueConfiguration config, long delayMillis) {
      super(name, muleContext, config);
      this.delayMillis = delayMillis;
    }

    @Override
    public Serializable poll(long timeout) throws InterruptedException {
      if (timeout == 0) {
        return super.poll(timeout);
      }

      if (timeout <= delayMillis) {
        sleep(timeout);
        return null;
      }

      sleep(delayMillis);
      return super.poll(timeout - delayMillis);
    }
  }

}
