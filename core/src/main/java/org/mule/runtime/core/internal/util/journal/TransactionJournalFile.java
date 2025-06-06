/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.journal;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

/**
 * Manages a transaction journal file.
 *
 * @param <T> type of the transaction identifier
 * @param <K> type of the journal entry
 */
class TransactionJournalFile<T, K extends JournalEntry<T>> {

  /**
   * Defines the minimum number of entries in the log that are required to clear the log file once there are no more transactions
   * pending.
   */
  private static final int MINIMUM_ENTRIES_TO_CLEAR_FILE = 10000;

  private static final Logger LOGGER = getLogger(TransactionJournalFile.class);

  private final File journalFile;
  private final JournalEntrySerializer<T, K> journalEntrySerializer;
  private final Long clearFileMinimumSizeInBytes;

  private final Multimap<T, K> entries = LinkedHashMultimap.create();

  private boolean doClear;
  private DataOutputStream logFileOutputStream;
  private int journalOperations = 0;

  /**
   * @param journalFile                  journal file to use. Will be created if it doesn't exists. If exists then transaction
   *                                     entries will get loaded from it.
   * @param journalEntrySerializer       serializer for {@link JournalEntry}
   * @param transactionCompletePredicate a callback to determine if a transaction is complete.
   */
  public TransactionJournalFile(File journalFile, JournalEntrySerializer<T, K> journalEntrySerializer,
                                TransactionCompletePredicate<T> transactionCompletePredicate, Long clearFileMinimumSizeInBytes) {
    this.journalFile = journalFile;
    this.journalEntrySerializer = journalEntrySerializer;
    this.clearFileMinimumSizeInBytes = clearFileMinimumSizeInBytes;
    if (journalFile.exists()) {
      doClear = true;
      loadAllEntries(transactionCompletePredicate);
    } else {
      // skip the clear call done for the recovery, since there's nothing to recover
      doClear = false;
    }

    createLogOutputStream();
  }

  /**
   * Adds a journal entry for an operation done over a transactional resource
   *
   * @param journalEntry operation details
   */
  public synchronized void logOperation(K journalEntry) {
    entries.put(journalEntry.getTxId(), journalEntry);
    journalEntrySerializer.serialize(journalEntry, logFileOutputStream);
    journalOperations++;
  }

  /**
   * Removes all journal entries for a particular transactions.
   *
   * If there are no transaction entries left then the journal file is emptied.
   *
   * @param txId transaction identifier
   */
  public synchronized void clearEntriesForTransaction(T txId) {
    doClearEntriesForTransaction(txId);
    clearFileIfNeeded();
  }

  protected void doClearEntriesForTransaction(T txId) {
    Collection<K> entries = this.entries.removeAll(txId);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Evicted from tx log file " + entries.size() + " entries from txid " + txId);
    }
  }

  protected void clearFileIfNeeded() {
    if (this.entries.isEmpty()) {
      if (clearFileMinimumSizeInBytes != null) {
        if (fileLength() > clearFileMinimumSizeInBytes) {
          clear();
          journalOperations = 0;
        }
      } else if (journalOperations > MINIMUM_ENTRIES_TO_CLEAR_FILE) {
        clear();
        journalOperations = 0;
      }
    }
  }

  /**
   * Free all resources held.
   *
   * Will not remove the transaction context.
   */
  public synchronized void close() {
    try {
      logFileOutputStream.close();
    } catch (IOException e) {
      LOGGER.warn(e.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Error closing transaction journal file", e);
      }
    }
  }

  /**
   * @param txId transaction identifier
   * @return a copy collection of the journal entries for txId
   */
  public Collection<K> getLogEntries(T txId) {
    final Collection<K> entries = this.entries.asMap().get(txId);
    if (entries == null) {
      return Collections.emptyList();
    }
    synchronized (entries) {
      return Collections.unmodifiableCollection(new ArrayList<>(entries));
    }
  }

  /**
   * @return all journal entries exactly as stored in the journal. No modifications should be done to such collections and the
   *         journal file should not be access concurrently for other purposes while working with the collection.
   */
  public synchronized Multimap<T, K> getAllLogEntries() {
    return entries;
  }

  /**
   * Remove all the entries from the transaction journal and cleans the transaction journal file.
   */
  public synchronized void clear() {
    if (doClear) {
      if (journalFile.exists()) {
        close();
        entries.clear();
        FileUtils.deleteQuietly(journalFile);
      }
      createLogOutputStream();
    } else {
      // after skipping the recovery, do clear normally
      doClear = true;
    }
  }

  private void createLogOutputStream() {
    if (!journalFile.exists()) {
      try {
        journalFile.createNewFile();
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
    }
    try {
      this.logFileOutputStream = new DataOutputStream(new FileOutputStream(journalFile, true));
    } catch (FileNotFoundException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * This will NOT load transactions that are already complete, according to the given {@code transactionCompletePredicate}.
   *
   * @param transactionCompletePredicate a callback to determine if a transaction is complete.
   */
  private void loadAllEntries(TransactionCompletePredicate<T> transactionCompletePredicate) {
    DataInputStream dataInputStream = null;
    try {
      dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(journalFile)));
      boolean logEntryCreationFailed = false;
      while (!logEntryCreationFailed) {
        try {
          K journalEntry = journalEntrySerializer.deserialize(dataInputStream);
          if (journalEntry != null) {
            this.entries.put(journalEntry.getTxId(), journalEntry);
            journalOperations++;

            if (transactionCompletePredicate.isTransactionComplete(journalEntry)) {
              journalOperations -= this.entries.get(journalEntry.getTxId()).size();
              doClearEntriesForTransaction(journalEntry.getTxId());
            }
          } else {
            logEntryCreationFailed = true;
          }
        } catch (EOFException e) {
          LOGGER.debug("Expected exception since there are no more log entries", e);
          logEntryCreationFailed = true;
        } catch (Exception e) {
          LOGGER.warn("Exception reading transaction content. This is normal if the mule server was shutdown due to a failure"
              + e.getMessage());
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Error reading transaction journal file", e);
          }
          logEntryCreationFailed = true;
        }
      }
      clearFileIfNeeded();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (dataInputStream != null) {
          dataInputStream.close();
        }
      } catch (IOException e) {
        LOGGER.error("Error loading transaction journal file entries", e);
      }
    }
  }

  /**
   * @return the number different transaction identifier held by the journal.
   */
  public int size() {
    return entries.size();
  }

  /**
   * @param txId transaction identifier
   * @return true if contains entries for that txId, false otherwise
   */
  public boolean containsTx(T txId) {
    return this.entries.containsKey(txId);
  }

  /**
   * @return the file length in bytes
   */
  public long fileLength() {
    return journalFile.length();
  }
}

