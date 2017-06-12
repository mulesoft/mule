/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.journal.queue;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.util.journal.JournalEntry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.transaction.xa.Xid;

/**
 * Queue {@link JournalEntry} for XA transactions
 */
public class XaQueueTxJournalEntry extends AbstractQueueTxJournalEntry<Xid> {

  public XaQueueTxJournalEntry(Xid txId, byte operation, String queueName, Serializable value) {
    super(new MuleXid(txId), operation, queueName, value);
  }

  public XaQueueTxJournalEntry(Xid txId, byte operation) {
    super(new MuleXid(txId), operation);
  }

  public XaQueueTxJournalEntry(DataInputStream inputStream, MuleContext muleContext) throws IOException {
    super(inputStream, muleContext);
  }

  @Override
  protected Xid deserializeTxId(DataInputStream inputStream) throws IOException {
    byte globalTransactionIdSize = inputStream.readByte();
    final byte[] globalTransactionId = new byte[globalTransactionIdSize];
    inputStream.read(globalTransactionId, 0, globalTransactionIdSize);
    byte branchQualifierIdSize = inputStream.readByte();
    final byte[] branchQualifierId = new byte[branchQualifierIdSize];
    inputStream.read(branchQualifierId, 0, branchQualifierIdSize);
    final int formadId = inputStream.readInt();
    return new MuleXid(formadId, globalTransactionId, branchQualifierId);
  }

  @Override
  protected void serializeTxId(DataOutputStream outputStream) throws IOException {
    outputStream.writeByte(getTxId().getGlobalTransactionId().length);
    outputStream.write(getTxId().getGlobalTransactionId());
    outputStream.writeByte(getTxId().getBranchQualifier().length);
    outputStream.write(getTxId().getBranchQualifier());
    outputStream.writeInt(getTxId().getFormatId());
  }

}
