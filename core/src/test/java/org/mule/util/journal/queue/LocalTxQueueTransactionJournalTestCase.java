/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.journal.queue;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.journal.JournalEntry;
import org.mule.util.queue.DefaultQueueStore;
import org.mule.util.queue.QueueStore;

import com.google.common.collect.Multimap;

import java.util.Collection;

import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

public class LocalTxQueueTransactionJournalTestCase extends AbstractMuleContextTestCase
{

    public static final int TX_ID = 1;
    public static final String QUEUE_NAME = "queueName";
    public static final String SOME_VALUE = "some value";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DefaultQueueStore mockQueueInfo = mock(DefaultQueueStore.class, Answers.RETURNS_DEEP_STUBS.get());


    @Before
    public void setUpMocks()
    {
        when(mockQueueInfo.getName()).thenReturn(QUEUE_NAME);
    }

    @Test
    public void logAddAndRetrieve() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(SOME_VALUE);
        LocalTxQueueTransactionJournal transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        transactionJournal.logAdd(TX_ID, mockQueueInfo, muleEvent);
        transactionJournal.close();
        transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
        assertThat(allEntries.size(), is(1));
        assertThat(allEntries.get(TX_ID).size(), is(1));
        LocalQueueTxJournalEntry logEntry = allEntries.get(TX_ID).iterator().next();
        assertThat(logEntry.getQueueName(), is(QUEUE_NAME));
        assertThat(((MuleEvent) logEntry.getValue()).getMessage().getPayloadAsString(), is(SOME_VALUE));
        assertThat(logEntry.isAdd(), is(true));
    }

    @Test
    public void logAddFirstAndRetrieve() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(SOME_VALUE);
        LocalTxQueueTransactionJournal transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        transactionJournal.logAddFirst(TX_ID, mockQueueInfo, muleEvent);
        transactionJournal.close();
        transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
        assertThat(allEntries.size(), is(1));
        assertThat(allEntries.get(TX_ID).size(), is(1));
        LocalQueueTxJournalEntry journalEntry = allEntries.get(TX_ID).iterator().next();
        assertThat(journalEntry.getQueueName(), is(QUEUE_NAME));
        assertThat(((MuleEvent) journalEntry.getValue()).getMessage().getPayloadAsString(), is(SOME_VALUE));
        assertThat(journalEntry.isAddFirst(), is(true));
    }

    @Test
    public void logRemoveAndRetrieve() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(SOME_VALUE);
        LocalTxQueueTransactionJournal transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        transactionJournal.logRemove(TX_ID, mockQueueInfo, muleEvent);
        transactionJournal.close();
        transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
        assertThat(allEntries.size(), is(1));
        assertThat(allEntries.get(TX_ID).size(), is(1));
        LocalQueueTxJournalEntry journalEntry = allEntries.get(TX_ID).iterator().next();
        assertThat(journalEntry.getQueueName(), is(QUEUE_NAME));
        assertThat(((MuleEvent) journalEntry.getValue()).getMessage().getPayloadAsString(), is(SOME_VALUE));
        assertThat(journalEntry.isRemove(), is(true));
    }

    @Test
    public void logCommitAndRetrieve()
    {
        LocalTxQueueTransactionJournal transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        transactionJournal.logCommit(TX_ID);
        transactionJournal.close();
        transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
        assertThat(allEntries.size(), is(1));
    }

    @Test
    public void logRollbackAndRetrieve()
    {
        LocalTxQueueTransactionJournal transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        transactionJournal.logRollback(TX_ID);
        transactionJournal.close();
        transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
        assertThat(allEntries.size(), is(1));
    }

    @Test
    public void logSeveralAddsThenCommitAndRetrieve() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(SOME_VALUE);
        LocalTxQueueTransactionJournal transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        int numberOfOffers = 1000;
        for (int i = 0; i < numberOfOffers; i++)
        {
            transactionJournal.logAdd(TX_ID, mockQueueInfo, muleEvent);
        }
        transactionJournal.logCommit(TX_ID);
        transactionJournal.close();
        transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
        assertThat(allEntries.size(), is(1001));
    }

    @Test
    public void logSeveralAddsAndRetrieve() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(SOME_VALUE);
        LocalTxQueueTransactionJournal transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        int numberOfOffers = 1000;
        for (int i = 0; i < numberOfOffers; i++)
        {
            transactionJournal.logAdd(TX_ID, mockQueueInfo, muleEvent);
        }
        transactionJournal.close();
        transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
        assertThat(allEntries.size(), is(numberOfOffers));
        assertThat(allEntries.get(TX_ID).size(), is(numberOfOffers));
        LocalQueueTxJournalEntry journalEntry = allEntries.get(TX_ID).iterator().next();
        assertThat(journalEntry.getQueueName(), is(QUEUE_NAME));
        assertThat(((MuleEvent) journalEntry.getValue()).getMessage().getPayloadAsString(), is(SOME_VALUE));
        assertThat(journalEntry.isAdd(), is(true));
    }

    @Test
    public void getTxEntriesReturnsACopy() throws Exception
    {
        LocalTxQueueTransactionJournal transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
        addTransactionJournalEntry(transactionJournal);
        addTransactionJournalEntry(transactionJournal);
        Collection<LocalQueueTxJournalEntry> logEntriesForTx = transactionJournal.getLogEntriesForTx(1);
        addTransactionJournalEntry(transactionJournal);
        Collection<LocalQueueTxJournalEntry> modifiedLogEntries = transactionJournal.getLogEntriesForTx(1);
        assertThat(logEntriesForTx, not(is(modifiedLogEntries)));
        assertThat(logEntriesForTx.size(), is(2));
    }

    private void addTransactionJournalEntry(LocalTxQueueTransactionJournal transactionJournal)
    {
        transactionJournal.logAdd(1, mockQueueInfo, "data");
    }

}
