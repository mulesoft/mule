/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.util.journal.queue.LocalTxQueueTransactionJournal;
import org.mule.runtime.core.util.journal.queue.LocalTxQueueTransactionRecoverer;
import org.mule.runtime.core.util.xa.ResourceManagerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalTxQueueTransactionRecovererTestCase extends AbstractMuleContextTestCase {

  public static final String QUEUE_NAME = "inQueue";
  private static final int TIMEOUT = 10;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private LocalTxQueueTransactionJournal txLog;
  private DefaultQueueStore inQueue;
  private PersistentQueueTransactionContext persistentTransactionContext;
  private LocalTxQueueTransactionRecoverer queueTransactionRecoverer;

  @Override
  protected void doSetUp() throws Exception {
    ((DefaultMuleConfiguration) muleContext.getConfiguration()).setWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
    txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    inQueue = new DefaultQueueStore(QUEUE_NAME, muleContext, new DefaultQueueConfiguration(0, true));
    persistentTransactionContext = new PersistentQueueTransactionContext(txLog, createQueueProvider(inQueue));
    queueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(txLog, createQueueProvider(inQueue));
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
    Event muleEvent = (Event) inQueue.poll(TIMEOUT);
    assertThat(muleEvent, notNullValue());
    assertThat(testEvent().getContext().getId(), equalTo(muleEvent.getContext().getId()));
  }

  @Test
  public void pollAndFailThenRecoverWithTwoElements() throws Exception {
    final String MESSAGE_CONTENT_2 = TEST_PAYLOAD + "2";
    Event testEvent2 = eventBuilder().message(of(MESSAGE_CONTENT_2)).build();

    inQueue.offer(testEvent(), 0, TIMEOUT);
    inQueue.offer(testEvent2, 0, TIMEOUT);
    Serializable value = persistentTransactionContext.poll(inQueue, 100000);
    assertThat(inQueue.getSize(), is(1));
    assertThat(value, notNullValue());
    txLog.close();
    txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    queueTransactionRecoverer.recover();
    Event muleEvent = (Event) inQueue.poll(TIMEOUT);
    assertThat(muleEvent, notNullValue());
    assertThat(muleEvent.getMessage().getPayload().getValue().toString(), is(MESSAGE_CONTENT_2)); // recovered element should be
                                                                                                  // last

    muleEvent = (Event) inQueue.poll(TIMEOUT);
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
    Event muleEvent = (Event) inQueue.poll(TIMEOUT);
    assertThat(muleEvent, notNullValue());
    assertThat(testEvent().getContext().getId(), equalTo(muleEvent.getContext().getId()));
    muleEvent = (Event) inQueue.poll(TIMEOUT);
    assertThat(muleEvent, nullValue());
  }

  private QueueProvider createQueueProvider(final DefaultQueueStore queue) {
    return new QueueProvider() {

      @Override
      public QueueStore getQueue(String queueName) {
        throw new NotImplementedException();
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
    persistentTransactionContext = new PersistentQueueTransactionContext(txLog, createQueueProvider(outQueue));
    persistentTransactionContext.offer(outQueue, testEvent(), TIMEOUT);
    assertThat(outQueue.poll(TIMEOUT), nullValue());
    txLog.close();
    txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    queueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(txLog, createQueueProvider(outQueue));
    queueTransactionRecoverer.recover();
    Serializable muleEvent = outQueue.poll(TIMEOUT);
    assertThat(muleEvent, nullValue());
  }

  @Test
  public void offerAndFailBetweenRealOfferAndCommitThenRecover() throws Exception {
    txLog = new TestTransactionLogger(temporaryFolder.getRoot().getAbsolutePath(), muleContext).failDuringLogCommit();
    final DefaultQueueStore outQueue = new DefaultQueueStore(QUEUE_NAME, muleContext, new DefaultQueueConfiguration(0, true));
    persistentTransactionContext = new PersistentQueueTransactionContext(txLog, createQueueProvider(outQueue));
    persistentTransactionContext.offer(outQueue, testEvent(), TIMEOUT);
    try {
      persistentTransactionContext.doCommit();
      fail();
    } catch (ResourceManagerException e) {
      // expected
    }
    txLog.close();
    txLog = new TestTransactionLogger(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    queueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(txLog, createQueueProvider(outQueue));
    queueTransactionRecoverer.recover();
    Serializable muleEvent = outQueue.poll(TIMEOUT);
    assertThat(muleEvent, nullValue());
  }

}
