/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.journal;

import org.mule.util.Preconditions;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Keeps track of transactional operation made over a transactional resource.
 *
 * Uses two files for keeping track of the transactions and empties a file once all the
 * entries in such file are resolved.
 *
 * Keeps a memory cache of the log entries for performance reasons.
 * Once a transaction finishes the client of this class must manually remove the
 * entries related to such transaction to clear the cache.
 */
public class TransactionJournal<T, K extends JournalEntry<T>>
{

    private static final int MAXIMUM_LOG_FILE_ENTRIES = 50000;

    private transient Log logger = LogFactory.getLog(getClass());

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
     * @param logFilesDirectory directory used to store the journal files.
     */
    public TransactionJournal(String logFilesDirectory, TransactionCompletePredicate transactionCompletePredicate, JournalEntrySerializer journalEntrySerializer)
    {
        File logFileDirectory = new File(logFilesDirectory);
        if (!logFileDirectory.exists())
        {
            Preconditions.checkState(logFileDirectory.mkdirs(), "Could not created directory for queue transaction logger " + logFileDirectory);
        }
        File logFile1 = new File(logFileDirectory, "tx1.log");
        File logFile2 = new File(logFileDirectory, "tx2.log");
        logger.info(String.format("Using files for tx logs %s and %s", logFile1.getAbsolutePath(), logFile2.getAbsolutePath()));

        this.currentLogFile = new TransactionJournalFile(logFile1, journalEntrySerializer);
        this.notCurrentLogFile = new TransactionJournalFile(logFile2, journalEntrySerializer);
        this.transactionCompletePredicate = transactionCompletePredicate;
    }

    /**
     * Log an update operation over a transactional resource
     *
     * @param journalEntry journal entry with the update operation details
     */
    public synchronized void logUpdateOperation(JournalEntry<T> journalEntry)
    {
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
    public synchronized void logCheckpointOperation(JournalEntry<T> journalEntry)
    {
        TransactionJournalFile logFile = determineLogFile(journalEntry.getTxId());
        logFile.logOperation(journalEntry);
        if (transactionCompletePredicate.isTransactionComplete(journalEntry))
        {
            logFile.clearEntriesForTransaction(journalEntry.getTxId());
        }
    }

    /**
     * @param txId transaction identifier
     * @return all the transaction entries for a certain transaction identifier
     */
    public Collection<K> getLogEntriesForTx(T txId)
    {
        TransactionJournalFile logFile = determineLogFileWithoutModifyingCurrent(txId);
        if (logFile == null || !logFile.containsTx(txId))
        {
            return Collections.emptyList();
        }
        return logFile.getLogEntries(txId);
    }

    /**
     * @return all the transactional entries from the journal
     */
    public synchronized Multimap<T, K> getAllLogEntries()
    {
        LinkedHashMultimap<T, K> logEntries = LinkedHashMultimap.create();
        logEntries.putAll(currentLogFile.getAllLogEntries());
        logEntries.putAll(notCurrentLogFile.getAllLogEntries());
        return logEntries;
    }

    /**
     * Release the resources used by the transaction journal
     */
    public synchronized void close()
    {
        currentLogFile.close();
        notCurrentLogFile.close();
    }

    /**
     * Removes all the entries from the transactional journal
     */
    public synchronized void clear()
    {
        currentLogFile.clear();
        notCurrentLogFile.clear();
    }

    private TransactionJournalFile determineLogFile(T txId)
    {
        final TransactionJournalFile logFile = determineLogFileWithoutModifyingCurrent(txId);
        if (logFile != null)
        {
            return logFile;
        }
        if (currentLogFile.size() > MAXIMUM_LOG_FILE_ENTRIES && notCurrentLogFile.size() == 0)
        {
            TransactionJournalFile aux = currentLogFile;
            currentLogFile = notCurrentLogFile;
            notCurrentLogFile = aux;
        }
        return currentLogFile;
    }

    private TransactionJournalFile determineLogFileWithoutModifyingCurrent(T txId)
    {
        if (currentLogFile.containsTx(txId))
        {
            return currentLogFile;
        }
        if (notCurrentLogFile.containsTx(txId))
        {
            return notCurrentLogFile;
        }
        return null;
    }

}
