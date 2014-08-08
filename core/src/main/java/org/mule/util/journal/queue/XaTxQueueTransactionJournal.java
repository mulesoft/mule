/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.journal.queue;

import org.mule.api.MuleContext;
import org.mule.util.journal.JournalEntrySerializer;
import org.mule.util.xa.MuleXid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

import javax.transaction.xa.Xid;

public class XaTxQueueTransactionJournal extends AbstractQueueTransactionJournal<Xid, XaQueueTxJournalEntry>
{

    public XaTxQueueTransactionJournal(String logFilesDirectory, final MuleContext muleContext)
    {
        super(logFilesDirectory, new JournalEntrySerializer<Xid, XaQueueTxJournalEntry>()
        {

            @Override
            public XaQueueTxJournalEntry deserialize(DataInputStream inputStream) throws IOException
            {
                return new XaQueueTxJournalEntry(inputStream, muleContext);
            }

            @Override
            public void serialize(XaQueueTxJournalEntry journalEntry, DataOutputStream dataOutputStream)
            {
                journalEntry.write(dataOutputStream, muleContext);
            }
        });
    }

    @Override
    protected XaQueueTxJournalEntry createUpdateJournalEntry(Xid txId, byte operation, String queueName, Serializable serialize)
    {
        return new XaQueueTxJournalEntry(txId, operation, queueName, serialize);
    }

    @Override
    protected XaQueueTxJournalEntry createCheckpointJournalEntry(Xid txId, byte operation)
    {
        return new XaQueueTxJournalEntry(txId, operation);
    }

    public void logPrepare(Xid xid)
    {
        getJournal().logCheckpointOperation(createCheckpointJournalEntry(xid, AbstractQueueTxJournalEntry.Operation.PREPARE.getByteRepresentation()));
    }

    @Override
    public Collection<XaQueueTxJournalEntry> getLogEntriesForTx(Xid txId)
    {
        return super.getLogEntriesForTx(new MuleXid(txId));
    }


}
