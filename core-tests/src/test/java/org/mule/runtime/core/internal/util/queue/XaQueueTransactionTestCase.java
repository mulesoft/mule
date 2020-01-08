/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.stubbing.Answer;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;
import org.mule.runtime.core.internal.util.journal.queue.MuleXid;
import org.mule.runtime.core.internal.util.journal.queue.XaTxQueueTransactionJournal;
import org.mule.runtime.core.internal.util.xa.XaTransactionRecoverer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.Serializable;
import java.util.concurrent.locks.Lock;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XaQueueTransactionTestCase extends AbstractMuleContextTestCase {

  public static final String QUEUE_NAME = "inQueue";
  private static final int TIMEOUT = 10;
  private static final Xid TEST_XID = new MuleXid(9, new byte[] {1, 2, 3, 4}, new byte[] {5, 6, 7, 8});

  private static final long QUEUE_DELAY_MILLIS = 2000;
  private static final long QUEUE_DELAY_TOLERANCE_MILLIS = 20;
  private DelayedQueueStore delayedQueue;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private XaTxQueueTransactionJournal txLog;
  private DefaultQueueStore inQueue;
  private PersistentXaTransactionContext persistentTransactionContext;
  private XaTransactionRecoverer xaTransactionRecoverer;
  private XaQueueTransactionContext localTxTransactionContext;

  @Override
  protected void doSetUp() throws Exception {
    ((DefaultMuleConfiguration) muleContext.getConfiguration()).setWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
    txLog = new XaTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);

    QueueConfiguration queueConfiguration = new DefaultQueueConfiguration(0, true);
    inQueue = new DefaultQueueStore(QUEUE_NAME, muleContext, queueConfiguration);
    delayedQueue = new DelayedQueueStore(QUEUE_NAME, muleContext, queueConfiguration, QUEUE_DELAY_MILLIS);

    persistentTransactionContext = new PersistentXaTransactionContext(txLog, createRecoverOnlyQueueProvider(inQueue), TEST_XID);
    xaTransactionRecoverer = new XaTransactionRecoverer(txLog, createRecoverOnlyQueueProvider(inQueue));
    localTxTransactionContext = createLocalTxContext(txLog, createQueueProvider(delayedQueue));
  }

  private XaQueueTransactionContext createLocalTxContext(XaTxQueueTransactionJournal txLog, QueueProvider provider)
      throws InterruptedException {
    Lock lock = mock(Lock.class);
    when(lock.tryLock(anyLong(), eq(MILLISECONDS))).thenAnswer((Answer<Boolean>) invocationOnMock -> {
      sleep(invocationOnMock.getArgument(0, Long.class) / 2);
      return true;
    });
    return new PersistentXaTransactionContext(txLog, provider, TEST_XID);
  }

  @Override
  protected void configureMuleContext(MuleContextBuilder contextBuilder) {
    DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
    muleConfiguration.setWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
    contextBuilder.setMuleConfiguration(muleConfiguration);
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
    persistentTransactionContext = new PersistentXaTransactionContext(txLog, createRecoverOnlyQueueProvider(outQueue), TEST_XID);
    persistentTransactionContext.offer(outQueue, testEvent(), TIMEOUT);
    assertThat(outQueue.poll(TIMEOUT), nullValue());
    txLog.close();
    txLog = new XaTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    xaTransactionRecoverer = new XaTransactionRecoverer(txLog, createRecoverOnlyQueueProvider(inQueue));
    xaTransactionRecoverer.recover(XAResource.TMSTARTRSCAN);
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
               anyOf(greaterThanOrEqualTo((double) timeout), closeTo(timeout, QUEUE_DELAY_TOLERANCE_MILLIS)));
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

    assertThat(elapsedMillis, lessThanOrEqualTo(QUEUE_DELAY_MILLIS + QUEUE_DELAY_TOLERANCE_MILLIS));
  }

  @Test
  public void offerDoesNotDelayMuchMoreThanGivenTimeout() throws InterruptedException, ResourceManagerException {
    long waitTime = QUEUE_DELAY_MILLIS / 2;
    int item = 1;

    long timeMillisBeforeOffer = currentTimeMillis();
    localTxTransactionContext.offer(delayedQueue, item, waitTime);
    localTxTransactionContext.doCommit();
    long elapsedMillis = currentTimeMillis() - timeMillisBeforeOffer;

    assertThat(elapsedMillis, lessThanOrEqualTo(waitTime + QUEUE_DELAY_TOLERANCE_MILLIS));
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
