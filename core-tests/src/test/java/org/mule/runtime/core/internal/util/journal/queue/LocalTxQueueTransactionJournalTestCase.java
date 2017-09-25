/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.journal.queue;

import static java.lang.Math.abs;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.util.journal.TransactionJournal.TX1_LOG_FILE_NAME;
import static org.mule.runtime.core.internal.util.journal.TransactionJournal.TX2_LOG_FILE_NAME;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.util.queue.DefaultQueueStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.google.common.collect.Multimap;

import java.io.File;
import java.util.Collection;
import java.util.Random;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

public class LocalTxQueueTransactionJournalTestCase extends AbstractMuleContextTestCase {

  private static final int TX_ID = 1;
  private static final String QUEUE_NAME = "queueName";
  private static final int ONE_KB = 1024;
  private static final long MAXIMUM_FILE_SIZE_EXPECTED = (512l + 100) * 1024l;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private DefaultQueueStore mockQueueInfo = mock(DefaultQueueStore.class, Answers.RETURNS_DEEP_STUBS.get());


  @Before
  public void setUpMocks() {
    when(mockQueueInfo.getName()).thenReturn(QUEUE_NAME);
  }

  @Test
  public void logAddAndRetrieve() throws Exception {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    transactionJournal.logAdd(TX_ID, mockQueueInfo, testEvent());
    transactionJournal.close();
    transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
    assertThat(allEntries.size(), is(1));
    assertThat(allEntries.get(TX_ID).size(), is(1));
    LocalQueueTxJournalEntry logEntry = allEntries.get(TX_ID).iterator().next();
    assertThat(logEntry.getQueueName(), is(QUEUE_NAME));
    assertThat(getPayloadAsString(((CoreEvent) logEntry.getValue()).getMessage()), is(TEST_PAYLOAD));
    assertThat(logEntry.isAdd(), is(true));
  }

  @Test
  public void logAddFirstAndRetrieve() throws Exception {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    transactionJournal.logAddFirst(TX_ID, mockQueueInfo, testEvent());
    transactionJournal.close();
    transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
    assertThat(allEntries.size(), is(1));
    assertThat(allEntries.get(TX_ID).size(), is(1));
    LocalQueueTxJournalEntry journalEntry = allEntries.get(TX_ID).iterator().next();
    assertThat(journalEntry.getQueueName(), is(QUEUE_NAME));
    assertThat(getPayloadAsString(((CoreEvent) journalEntry.getValue()).getMessage()), is(TEST_PAYLOAD));
    assertThat(journalEntry.isAddFirst(), is(true));
  }

  @Test
  public void logRemoveAndRetrieve() throws Exception {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    transactionJournal.logRemove(TX_ID, mockQueueInfo, testEvent());
    transactionJournal.close();
    transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
    assertThat(allEntries.size(), is(1));
    assertThat(allEntries.get(TX_ID).size(), is(1));
    LocalQueueTxJournalEntry journalEntry = allEntries.get(TX_ID).iterator().next();
    assertThat(journalEntry.getQueueName(), is(QUEUE_NAME));
    assertThat(getPayloadAsString(((CoreEvent) journalEntry.getValue()).getMessage()), is(TEST_PAYLOAD));
    assertThat(journalEntry.isRemove(), is(true));
  }

  @Test
  public void logCommitAndRetrieve() {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    transactionJournal.logCommit(TX_ID);
    transactionJournal.close();
    transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
    assertThat(allEntries.size(), is(0));
  }

  @Test
  public void logRollbackAndRetrieve() {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    transactionJournal.logRollback(TX_ID);
    transactionJournal.close();
    transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
    assertThat(allEntries.size(), is(0));
  }

  @Test
  public void logSeveralAddsThenCommitAndRetrieve() throws Exception {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    int numberOfOffers = 1000;
    for (int i = 0; i < numberOfOffers; i++) {
      transactionJournal.logAdd(TX_ID, mockQueueInfo, testEvent());
    }
    transactionJournal.logCommit(TX_ID);
    transactionJournal.close();
    transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
    assertThat(allEntries.size(), is(0));
  }

  @Test
  public void logSeveralAddsThenRetrieveAndCommit() throws Exception {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    int numberOfOffers = 1000;
    for (int i = 0; i < numberOfOffers; i++) {
      transactionJournal.logAdd(TX_ID, mockQueueInfo, testEvent());
    }
    transactionJournal.close();
    transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    transactionJournal.logCommit(TX_ID);
    Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
    assertThat(allEntries.size(), is(0));
  }

  @Test
  public void logSeveralAddsAndRetrieve() throws Exception {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    int numberOfOffers = 1000;
    for (int i = 0; i < numberOfOffers; i++) {
      transactionJournal.logAdd(TX_ID, mockQueueInfo, testEvent());
    }
    transactionJournal.close();
    transactionJournal = new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    Multimap<Integer, LocalQueueTxJournalEntry> allEntries = transactionJournal.getAllLogEntries();
    assertThat(allEntries.size(), is(numberOfOffers));
    assertThat(allEntries.get(TX_ID).size(), is(numberOfOffers));
    LocalQueueTxJournalEntry journalEntry = allEntries.get(TX_ID).iterator().next();
    assertThat(journalEntry.getQueueName(), is(QUEUE_NAME));
    assertThat(getPayloadAsString(((CoreEvent) journalEntry.getValue()).getMessage()), is(TEST_PAYLOAD));
    assertThat(journalEntry.isAdd(), is(true));
  }

  @Test
  public void getTxEntriesReturnsACopy() throws Exception {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext);
    addTransactionJournalEntry(transactionJournal);
    addTransactionJournalEntry(transactionJournal);
    Collection<LocalQueueTxJournalEntry> logEntriesForTx = transactionJournal.getLogEntriesForTx(1);
    addTransactionJournalEntry(transactionJournal);
    Collection<LocalQueueTxJournalEntry> modifiedLogEntries = transactionJournal.getLogEntriesForTx(1);
    assertThat(logEntriesForTx, not(is(modifiedLogEntries)));
    assertThat(logEntriesForTx.size(), is(2));
  }

  @Test
  public void maximumFileSizeGreaterThanZero() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext, 0);
  }

  @Test
  public void changeFileWhenMaximumExceeded() throws Exception {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext, 1);
    // log different tx ids so both files will grow.
    for (int i = 0; i < 7; i++) {
      logAddWith100kbEntry(transactionJournal, i);
    }
    File logFile1 = getFirstLogFile();
    File logFile2 = getSecondLogFile();
    assertLogFileIsWithinBoundaries(logFile1);
    // second log file should have entries by now.
    assertThat(logFile2.length(), greaterThan(0l));
    for (int i = 0; i < 7; i++) {
      transactionJournal.logCommit(i);
    }
    // since all transactions were resolved and this file exceeded minimum size for clear, then it should be empty
    assertThat(logFile1.length(), is(0l));
    // this file didn't exceeded minimum size for clear, so it should have content
    assertThat(logFile2.length(), greaterThan(0l));
  }

  @Test
  public void doNotExceedMaximumByFar() throws Exception {
    LocalTxQueueTransactionJournal transactionJournal =
        new LocalTxQueueTransactionJournal(temporaryFolder.getRoot().getAbsolutePath(), muleContext, 1);
    for (int i = 0; i < 100; i++) {
      addSeveralEntriesToLogFile(transactionJournal);
      File logFile1 = getFirstLogFile();
      File logFile2 = getSecondLogFile();
      assertLogFileIsWithinBoundaries(logFile1);
      assertLogFileIsWithinBoundaries(logFile2);
    }
  }

  private void assertLogFileIsWithinBoundaries(File file) {
    assertThat(file.length(), lessThan(MAXIMUM_FILE_SIZE_EXPECTED));
  }

  private void addSeveralEntriesToLogFile(LocalTxQueueTransactionJournal transactionJournal) {
    for (int i = 0; i < 5; i++) {
      int txId = abs(new Random().nextInt());
      logAddWith100kbEntry(transactionJournal, txId);
      transactionJournal.logCommit(txId);
    }
  }

  private void logAddWith100kbEntry(LocalTxQueueTransactionJournal transactionJournal, int txId) {
    byte[] data = new byte[ONE_KB * 100];
    transactionJournal.logAdd(txId, mockQueueInfo, data);
  }

  private File getSecondLogFile() {
    return new File(temporaryFolder.getRoot().getAbsolutePath(), TX2_LOG_FILE_NAME);
  }

  private File getFirstLogFile() {
    return new File(temporaryFolder.getRoot().getAbsolutePath(), TX1_LOG_FILE_NAME);
  }

  private void addTransactionJournalEntry(LocalTxQueueTransactionJournal transactionJournal) {
    transactionJournal.logAdd(1, mockQueueInfo, "data");
  }

}
