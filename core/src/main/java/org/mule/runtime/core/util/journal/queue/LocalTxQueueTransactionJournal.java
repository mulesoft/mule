/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.journal.queue;

import org.mule.api.MuleContext;
import org.mule.util.journal.JournalEntrySerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Journal for operations done to a queue within a transactional context
 */
public class LocalTxQueueTransactionJournal extends AbstractQueueTransactionJournal<Integer, LocalQueueTxJournalEntry>
{

    public LocalTxQueueTransactionJournal(String logFilesDirectory, final MuleContext muleContext)
    {
        super(logFilesDirectory, createLocalTxQueueJournalEntrySerializer(muleContext));
    }

    public static JournalEntrySerializer<Integer, LocalQueueTxJournalEntry> createLocalTxQueueJournalEntrySerializer(final MuleContext muleContext)
    {
        return new JournalEntrySerializer<Integer, LocalQueueTxJournalEntry>(){

            @Override
            public LocalQueueTxJournalEntry deserialize(DataInputStream inputStream) throws IOException
            {
                return new LocalQueueTxJournalEntry(inputStream, muleContext);
            }

            @Override
            public void serialize(LocalQueueTxJournalEntry journalEntry, DataOutputStream dataOutputStream)
            {
                journalEntry.write(dataOutputStream, muleContext);
            }
        };
    }

    @Override
    protected LocalQueueTxJournalEntry createUpdateJournalEntry(Integer txId, byte byteRepresentation, String queueName, Serializable value)
    {
        return new LocalQueueTxJournalEntry(txId, byteRepresentation, queueName, value);
    }

    @Override
    protected LocalQueueTxJournalEntry createCheckpointJournalEntry(Integer txId, byte operation)
    {
        return new LocalQueueTxJournalEntry(txId, operation);
    }

}
