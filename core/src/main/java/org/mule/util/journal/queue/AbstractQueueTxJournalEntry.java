/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.journal.queue;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.journal.JournalEntry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Base implementation for a queue {@link org.mule.util.journal.JournalEntry}.
 *
 * @param <T> type of the entry identifier
 */
public abstract class AbstractQueueTxJournalEntry<T> implements JournalEntry<T>
{

    enum Operation
    {
        COMMIT((byte) 1), ROLLBACK((byte) 2), PREPARE((byte) 3), REMOVE((byte) 4), ADD((byte) 5), ADD_FIRST((byte) 6);

        private final byte byteRepresentation;

        Operation(byte operation)
        {
            this.byteRepresentation = operation;
        }

        public byte getByteRepresentation()
        {
            return byteRepresentation;
        }

        public static Operation createFromByteRepresentation(byte byteRepresentation)
        {
            for (Operation operation : values())
            {
                if (operation.byteRepresentation == byteRepresentation)
                {
                    return operation;
                }
            }
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("Unexpected byte representation value: " + byteRepresentation));
        }
    }

    private T txId;
    private String queueName;
    private byte operation;
    private Serializable value;

    public AbstractQueueTxJournalEntry(T txId, byte operation, String queueName, Serializable value)
    {
        this.txId = txId;
        this.queueName = queueName;
        this.operation = operation;
        this.value = value;
    }

    public AbstractQueueTxJournalEntry(T txId, byte operation)
    {
        this.txId = txId;
        this.operation = operation;
    }

    public AbstractQueueTxJournalEntry(DataInputStream inputStream, MuleContext muleContext) throws IOException
    {
        txId = deserializeTxId(inputStream);
        operation = inputStream.readByte();
        if (isCheckpointOperation(operation))
        {
            return;
        }

        int queueNameSize = toUnsignedInt(inputStream.readByte());
        byte[] queueNameAsBytes = new byte[queueNameSize];
        inputStream.read(queueNameAsBytes, 0, queueNameSize);
        int valueSize = inputStream.readInt();
        byte[] valueAsBytes = new byte[valueSize];
        inputStream.read(valueAsBytes, 0, valueSize);
        queueName = new String(queueNameAsBytes);
        value = muleContext.getObjectSerializer().deserialize(valueAsBytes);
    }

    public void write(DataOutputStream outputStream, MuleContext muleContext)
    {
        try
        {
            serializeTxId(outputStream);
            outputStream.write(operation);
            if (isCheckpointOperation(operation))
            {
                outputStream.flush();
                return;
            }
            outputStream.write(queueName.length());
            outputStream.write(queueName.getBytes());
            byte[] serializedValue = muleContext.getObjectSerializer().serialize(value);
            outputStream.writeInt(serializedValue.length);
            outputStream.write(serializedValue);
            outputStream.flush();
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    public static boolean isCheckpointOperation(byte operationAsByte)
    {
        Operation operation = Operation.createFromByteRepresentation(operationAsByte);
        return operation.equals(Operation.COMMIT) || operation.equals(Operation.ROLLBACK) || operation.equals(Operation.PREPARE);
    }

    public Serializable getValue()
    {
        return value;
    }

    public String getQueueName()
    {
        return queueName;
    }

    public T getTxId()
    {
        return txId;
    }

    public byte getOperation()
    {
        return operation;
    }

    public boolean isCommit()
    {
        return operation == Operation.COMMIT.getByteRepresentation();
    }

    public boolean isRollback()
    {
        return operation == Operation.ROLLBACK.getByteRepresentation();
    }

    public boolean isRemove()
    {
        return operation == Operation.REMOVE.getByteRepresentation();
    }

    public boolean isAdd()
    {
        return operation == Operation.ADD.getByteRepresentation();
    }

    public boolean isAddFirst()
    {
        return operation == Operation.ADD_FIRST.getByteRepresentation();
    }

    public boolean isPrepare()
    {
        return Operation.PREPARE.getByteRepresentation() == getOperation();
    }

    private int toUnsignedInt(byte b)
    {
        return b & 0xFF;
    }

    /**
     * @param inputStream stream in from which the transaction id must be deserialized
     * @return the deserialized transaction identifier
     * @throws IOException in case the deserialization fails
     */
    protected abstract T deserializeTxId(DataInputStream inputStream) throws IOException;

    /**
     * @param outputStream stream used to serialize the transaction identifier
     * @throws IOException in case the serialization fails
     */
    protected abstract void serializeTxId(DataOutputStream outputStream) throws IOException;
}

