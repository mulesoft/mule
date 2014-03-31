/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextBuilder;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.journal.queue.LocalTxQueueTransactionJournal;
import org.mule.util.journal.queue.LocalTxQueueTransactionRecoverer;
import org.mule.util.xa.ResourceManagerException;

import java.io.Serializable;

import org.apache.commons.lang.NotImplementedException;
import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalTxQueueTransactionRecovererTestCase extends AbstractMuleContextTestCase
{

    public static final String QUEUE_NAME = "inQueue";
    public static final String MESSAGE_CONTENT = "data";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private DefaultQueueStore inQueue;


    @Override
    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
        muleConfiguration.setWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
        contextBuilder.setMuleConfiguration(muleConfiguration);
    }

    @Test
    public void pollAndFailThenRecover() throws Exception
    {
        ((DefaultMuleConfiguration) muleContext.getConfiguration()).setWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
        LocalTxQueueTransactionJournal txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        inQueue = new DefaultQueueStore(QUEUE_NAME, muleContext, new DefaultQueueConfiguration(0, true));
        PersistentQueueTransactionContext persistentTransactionContext = new PersistentQueueTransactionContext(txLog, createQueueProvider(inQueue));
        MuleEvent testEvent = getTestEvent(MESSAGE_CONTENT);
        inQueue.offer(testEvent, 0, 10);
        Serializable value = persistentTransactionContext.poll(inQueue, 100000);
        assertThat(inQueue.poll(10), IsNull.nullValue());
        assertThat(value, notNullValue());
        txLog.close();
        txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        LocalTxQueueTransactionRecoverer queueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(txLog, createQueueProvider(inQueue));
        queueTransactionRecoverer.recover();
        Serializable muleEvent = inQueue.poll(10);
        assertThat(muleEvent, notNullValue());
        assertThat(testEvent.equals(muleEvent), is(true));
    }

    @Test
    public void failBetweenLogEntryWriteAndRealPoolThenRecover() throws Exception
    {
        ((DefaultMuleConfiguration) muleContext.getConfiguration()).setWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
        LocalTxQueueTransactionJournal txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        final DefaultQueueStore inQueue = new DefaultQueueStore(QUEUE_NAME, muleContext, new DefaultQueueConfiguration(0, true));
        PersistentQueueTransactionContext persistentTransactionContext = new PersistentQueueTransactionContext(txLog, createQueueProvider(inQueue));
        MuleEvent testEvent = getTestEvent(MESSAGE_CONTENT);
        inQueue.offer(testEvent, 0, 10);
        persistentTransactionContext.poll(inQueue, 10);
        txLog.close();
        txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        LocalTxQueueTransactionRecoverer queueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(txLog, createQueueProvider(inQueue));
        queueTransactionRecoverer.recover();
        Serializable muleEvent = inQueue.poll(10);
        assertThat(muleEvent, notNullValue());
        assertThat(testEvent.equals(muleEvent), is(true));
        muleEvent = inQueue.poll(10);
        assertThat(muleEvent, IsNull.nullValue());
    }

    private QueueProvider createQueueProvider(final DefaultQueueStore queue)
    {
        return new QueueProvider()
        {
            @Override
            public QueueStore getQueue(String queueName)
            {
                throw new NotImplementedException();
            }

            @Override
            public RecoverableQueueStore getRecoveryQueue(String queueName)
            {
                return queue;
            }
        };
    }

    @Test
    public void offerAndFailThenRecover() throws Exception
    {
        ((DefaultMuleConfiguration) muleContext.getConfiguration()).setWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
        LocalTxQueueTransactionJournal txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        final DefaultQueueStore outQueue = new DefaultQueueStore(QUEUE_NAME, muleContext, new DefaultQueueConfiguration(0, true));
        PersistentQueueTransactionContext persistentTransactionContext = new PersistentQueueTransactionContext(txLog, createQueueProvider(outQueue));
        MuleEvent testEvent = getTestEvent(MESSAGE_CONTENT);
        persistentTransactionContext.offer(outQueue, testEvent, 10);
        assertThat(outQueue.poll(10), IsNull.nullValue());
        txLog.close();
        txLog = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        LocalTxQueueTransactionRecoverer queueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(txLog, createQueueProvider(outQueue));
        queueTransactionRecoverer.recover();
        Serializable muleEvent = outQueue.poll(10);
        assertThat(muleEvent, nullValue());
    }

    @Test
    public void offerAndFailBetweenRealOfferAndCommitThenRecover() throws Exception
    {
        ((DefaultMuleConfiguration) muleContext.getConfiguration()).setWorkingDirectory(temporaryFolder.getRoot().getAbsolutePath());
        TestTransactionLogger txLog = new TestTransactionLogger(temporaryFolder.getRoot().getAbsolutePath(), muleContext).failDuringLogCommit();
        final DefaultQueueStore outQueue = new DefaultQueueStore(QUEUE_NAME, muleContext, new DefaultQueueConfiguration(0, true));
        PersistentQueueTransactionContext persistentTransactionContext = new PersistentQueueTransactionContext(txLog, createQueueProvider(outQueue));
        MuleEvent testEvent = getTestEvent(MESSAGE_CONTENT);
        persistentTransactionContext.offer(outQueue, testEvent, 10);
        try
        {
            persistentTransactionContext.doCommit();
            fail();
        }
        catch (ResourceManagerException e)
        {
            //expected
        }
        txLog.close();
        txLog = new TestTransactionLogger(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        LocalTxQueueTransactionRecoverer queueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(txLog, createQueueProvider(outQueue));
        queueTransactionRecoverer.recover();
        Serializable muleEvent = outQueue.poll(10);
        assertThat(muleEvent, nullValue());
    }

}
