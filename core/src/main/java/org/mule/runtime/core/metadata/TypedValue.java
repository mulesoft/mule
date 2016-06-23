/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.metadata;

import org.mule.runtime.api.metadata.DataType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Maintains a value that has an associated {@link DataType}
 */
public class TypedValue<T> implements Serializable
{

    private static final long serialVersionUID = -2533879516750283994L;

    private transient T value;

    // TODO MULE-9279 Make 'final' once MuleMessage is immutable
    private DataType<T> dataType;

    public TypedValue(T value, DataType<T> dataType)
    {
        this.value = value;
        if (dataType == null)
        {
            this.dataType = DataType.builder(dataType).type((Class<T>) value.getClass()).build();
        }
        else
        {
            this.dataType = dataType;
        }
    }

    // TODO MULE-9279 Add generics type once MuleMessage is immutable
    public DataType<?> getDataType()
    {
        return dataType;
    }

    public T getValue()
    {
        return value;
    }

    // TODO MULE-9279 Remove method once MuleMessage is immutable
    protected void setDataType(DataType<T> dataType)
    {
        this.dataType = dataType;
    }

    // TODO MULE-9279 Remove method once MuleMessage is immutable
    protected void setValue(T value)
    {
        this.value = value;
    }

    /**
     * Performs serialization of the value.  The is external to #writeObject to allow it to be overriden in sub-classes.
     */
    protected void serializeValue(ObjectOutputStream out) throws Exception
    {
        out.writeObject(value);
    }

    /**
     * Performs deserialization of the value.  The is external to #writeObject to allow it to be overriden in sub-classes.
     */
    protected void deserializeValue(ObjectInputStream out) throws Exception
    {
        value = (T) out.readObject();
    }

    private void writeObject(ObjectOutputStream out) throws Exception
    {
        out.defaultWriteObject();
        serializeValue(out);
    }

    private void readObject(ObjectInputStream in) throws Exception
    {
        in.defaultReadObject();
        deserializeValue(in);
    }

}
