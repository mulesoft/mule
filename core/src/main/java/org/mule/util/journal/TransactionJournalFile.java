/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.journal;

import org.mule.api.MuleRuntimeException;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manages a transaction journal file.
 *
 * @param <T> type of the transaction identifier
 * @param <K> type of the journal entry
 */
class TransactionJournalFile<T, K extends JournalEntry<T>>
{

    /**
     * Defines the minimum number of entries in the log that are required
     * to clear the log file once there are no more transactions pending.
     */
    private static final int MINIMUM_ENTRIES_TO_CLEAR_FILE = 10000;

    protected transient Log logger = LogFactory.getLog(getClass());

    private final File journalFile;
    private final JournalEntrySerializer<T, K> journalEntrySerializer;

    private Multimap<T, K> entries = LinkedHashMultimap.create();

    private DataOutputStream logFileOutputStream;
    private int journalOperations = 0;

    /**
     * @param journalFile journal file to use. Will be created if it doesn't exists. If exists then transaction entries will get loaded from it.
     * @param journalEntrySerializer serializer for {@link org.mule.util.journal.JournalEntry}
     */
    public TransactionJournalFile(File journalFile, JournalEntrySerializer journalEntrySerializer)
    {
        this.journalFile = journalFile;
        this.journalEntrySerializer = journalEntrySerializer;
        if (journalFile.exists())
        {
            loadAllEntries();
        }
        createLogOutputStream();
    }

    /**
     * Adds a journal entry for an operation done over a transactional resource
     * @param journalEntry operation details
     */
    public synchronized void logOperation(K journalEntry)
    {
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
    public synchronized void clearEntriesForTransaction(T txId)
    {
        Collection<K> entries = this.entries.removeAll(txId);
        if (logger.isDebugEnabled())
        {
            logger.debug("Evicted from tx log file " + entries.size() + " entries from txid " + txId);
        }
        if (this.entries.isEmpty())
        {
            if (journalOperations > MINIMUM_ENTRIES_TO_CLEAR_FILE)
            {
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
    public synchronized void close()
    {
        try
        {
            logFileOutputStream.close();
        }
        catch (IOException e)
        {
            logger.warn(e.getMessage());
            if (logger.isDebugEnabled())
            {
                logger.debug(e);
            }
        }
    }

    /**
     * @param txId transaction identifier
     * @return a copy collection of the journal entries for txId
     */
    public Collection<K> getLogEntries(T txId)
    {
        final Collection<K> entries = this.entries.asMap().get(txId);
        if (entries == null)
        {
            return Collections.emptyList();
        }
        synchronized (entries)
        {
            return Collections.unmodifiableCollection(new ArrayList<K>(entries));
        }
    }

    /**
     * @return all journal entries exactly as stored in the journal.
     * No modifications should be done to such collections and the journal file should not
     * be access concurrently for other purposes while working with the collection.
     */
    public synchronized Multimap<T, K> getAllLogEntries()
    {
        return entries;
    }

    /**
     * Remove all the entries from the transaction journal and cleans the transaction journal fle.
     */
    public synchronized void clear()
    {
        close();
        entries.clear();
        FileUtils.deleteQuietly(journalFile);
        createLogOutputStream();
    }

    private void createLogOutputStream()
    {
        if (!journalFile.exists())
        {
            try
            {
                journalFile.createNewFile();
            }
            catch (IOException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
        try
        {
            this.logFileOutputStream = new DataOutputStream(new FileOutputStream(journalFile, true));
        }
        catch (FileNotFoundException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private void loadAllEntries()
    {
        if (!journalFile.exists())
        {
            return;
        }
        DataInputStream dataInputStream = null;
        try
        {
            dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(journalFile)));
            boolean logEntryCreationFailed = false;
            while (!logEntryCreationFailed)
            {
                try
                {
                    K journalEntry = journalEntrySerializer.deserialize(dataInputStream);
                    if (journalEntry != null)
                    {
                        this.entries.put(journalEntry.getTxId(), journalEntry);
                    }
                    else
                    {
                        logEntryCreationFailed = true;
                    }
                }
                catch (EOFException e)
                {
                    logger.debug("Expected exception since there are no more log entries",e);
                    logEntryCreationFailed = true;
                }
                catch(Exception e)
                {
                    throw new MuleRuntimeException(e);
                }
            }
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                if (dataInputStream != null)
                {
                    dataInputStream.close();
                }
            }
            catch (IOException e)
            {
                logger.error(e);
            }
        }
    }

    /**
     * @return the number different transaction identifier held by the journal.
     */
    public int size()
    {
        return entries.size();
    }

    /**
     * @param txId transaction identifier
     * @return true if contains entries for that txId, false otherwise
     */
    public boolean containsTx(T txId)
    {
        return this.entries.containsKey(txId);
    }

}

