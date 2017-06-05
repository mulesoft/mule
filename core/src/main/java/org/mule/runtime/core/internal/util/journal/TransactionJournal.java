/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.journal;

import org.mule.runtime.api.util.Preconditions;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of transactional operation made over a transactional resource.
 *
 * Uses two files for keeping track of the transactions and empties a file once all the entries in such file are resolved.
 *
 * Keeps a memory cache of the log entries for performance reasons. Once a transaction finishes the client of this class must
 * manually remove the entries related to such transaction to clear the cache.
 */
public class TransactionJournal<T, K extends JournalEntry<T>> {

  public static final String TX1_LOG_FILE_NAME = "tx1.log";
  public static final String TX2_LOG_FILE_NAME = "tx2.log";

  private static final int MAXIMUM_LOG_FILE_ENTRIES = 50000;
  private static final int ONE_MEGABYTE_IN_BYTES = 1024 * 1024;

  private transient Logger logger = LoggerFactory.getLogger(getClass());

  private final TransactionCompletePredicate transactionCompletePredicate;

  /**
   * Log file in which we are currently writing new entries.
   */
  private TransactionJournalFile<T, K> currentLogFile;
  /**
   * Log file which has old entries and will be cleared as soon as all the transaction in it are resolved.
   */
  private TransactionJournalFile<T, K> notCurrentLogFile;

  /**
   * Maximum transaction log file size in bytes.
   */
  private Long maximumFileSizeInBytes;

  /**
   * Minimum transaction log file size in bytes to consider it ready for clear content.
   */
  private Long clearFileMinimumSizeInBytes;

  /**
   * @param logFilesDirectory directory used to store the journal files.
   */
  public TransactionJournal(String logFilesDirectory, TransactionCompletePredicate transactionCompletePredicate,
                            JournalEntrySerializer journalEntrySerializer, Integer maximumFileSizeInMegabytes) {
    File logFileDirectory = new File(logFilesDirectory);
    if (!logFileDirectory.exists()) {
      Preconditions.checkState(logFileDirectory.mkdirs(),
                               "Could not create directory for queue transaction logger " + logFileDirectory);
    }
    calculateJournalFileSize(maximumFileSizeInMegabytes);
    File logFile1 = new File(logFileDirectory, TX1_LOG_FILE_NAME);
    File logFile2 = new File(logFileDirectory, TX2_LOG_FILE_NAME);
    logger.info(String.format("Using files for tx logs %s and %s", logFile1.getAbsolutePath(), logFile2.getAbsolutePath()));

    this.currentLogFile =
        new TransactionJournalFile(logFile1, journalEntrySerializer, transactionCompletePredicate, clearFileMinimumSizeInBytes);
    this.notCurrentLogFile =
        new TransactionJournalFile(logFile2, journalEntrySerializer, transactionCompletePredicate, clearFileMinimumSizeInBytes);
    this.transactionCompletePredicate = transactionCompletePredicate;

  }

  private void calculateJournalFileSize(Integer maximumFileSizeInMegabytes) {
    if (maximumFileSizeInMegabytes != null) {
      this.maximumFileSizeInBytes = ((long) maximumFileSizeInMegabytes * ONE_MEGABYTE_IN_BYTES) / 2;
      this.clearFileMinimumSizeInBytes = this.maximumFileSizeInBytes / 2;
    }
  }

  /**
   * Log an update operation over a transactional resource
   *
   * @param journalEntry journal entry with the update operation details
   */
  public synchronized void logUpdateOperation(JournalEntry<T> journalEntry) {
    TransactionJournalFile logFile = determineLogFile(journalEntry.getTxId());
    logFile.logOperation(journalEntry);
  }

  /**
   * Logs a checkpoint operation over the transactions.
   *
   * Most likely this is an operation such as COMMIT, ROLLBACK or PREPARE of a TX.
   *
   * @param journalEntry journal entry with the checkpoint operation details
   */
  public synchronized void logCheckpointOperation(JournalEntry<T> journalEntry) {
    TransactionJournalFile logFile = determineLogFile(journalEntry.getTxId());
    logFile.logOperation(journalEntry);
    if (transactionCompletePredicate.isTransactionComplete(journalEntry)) {
      logFile.clearEntriesForTransaction(journalEntry.getTxId());
    }
  }

  /**
   * @param txId transaction identifier
   * @return all the transaction entries for a certain transaction identifier
   */
  public Collection<K> getLogEntriesForTx(T txId) {
    TransactionJournalFile logFile = determineLogFileWithoutModifyingCurrent(txId);
    if (logFile == null || !logFile.containsTx(txId)) {
      return Collections.emptyList();
    }
    return logFile.getLogEntries(txId);
  }

  /**
   * @return all the transactional entries from the journal
   */
  public synchronized Multimap<T, K> getAllLogEntries() {
    LinkedHashMultimap<T, K> logEntries = LinkedHashMultimap.create();
    logEntries.putAll(currentLogFile.getAllLogEntries());
    logEntries.putAll(notCurrentLogFile.getAllLogEntries());
    return logEntries;
  }

  /**
   * Release the resources used by the transaction journal
   */
  public synchronized void close() {
    currentLogFile.close();
    notCurrentLogFile.close();
  }

  /**
   * Removes all the entries from the transactional journal
   */
  public synchronized void clear() {
    currentLogFile.clear();
    notCurrentLogFile.clear();
  }

  private TransactionJournalFile determineLogFile(T txId) {
    final TransactionJournalFile logFile = determineLogFileWithoutModifyingCurrent(txId);
    if (logFile != null) {
      return logFile;
    }
    // we keep this condition for backward compatibility.
    if (maximumFileSizeInBytes == null) {
      if (currentLogFile.size() > MAXIMUM_LOG_FILE_ENTRIES && notCurrentLogFile.size() == 0) {
        debugLogFilesSwap();
        changeCurrentLogFile();
      }
    } else {
      if (currentLogFile.fileLength() > maximumFileSizeInBytes && notCurrentLogFile.size() == 0) {
        debugLogFilesSwap();
        changeCurrentLogFile();
      }
    }
    return currentLogFile;
  }

  private void debugLogFilesSwap() {
    if (logger.isDebugEnabled()) {
      logger.debug("Changing files, current file size: " + currentLogFile.fileLength() + " other file size: "
          + notCurrentLogFile.fileLength());
    }
  }

  private void changeCurrentLogFile() {
    TransactionJournalFile aux = currentLogFile;
    currentLogFile = notCurrentLogFile;
    notCurrentLogFile = aux;
  }

  private TransactionJournalFile determineLogFileWithoutModifyingCurrent(T txId) {
    if (currentLogFile.containsTx(txId)) {
      return currentLogFile;
    }
    if (notCurrentLogFile.containsTx(txId)) {
      return notCurrentLogFile;
    }
    return null;
  }

}
