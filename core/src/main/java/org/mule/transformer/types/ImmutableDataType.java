/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.types;

import org.mule.api.transformer.DataType;

/** A wrapper for a datatype that ensures it cannot be changed.  Get a non-immutable version by calling
 * cloneDataType().
  */
class ImmutableDataType<T> implements DataType<T>
{
    private DataType<T> theDataType;

    /**
     * Wrap a DataType with immutability.
     */
    public ImmutableDataType(DataType<T> theDataType)
    {
        this.theDataType = theDataType;
    }


    // These simply delegate
    public Class<?> getType()
    {
        return theDataType.getType();
    }

    public String getMimeType()
    {
        return theDataType.getMimeType();
    }

    public String getEncoding()
    {
        return theDataType.getEncoding();
    }

    public boolean isCompatibleWith(DataType dataType)
    {
        return theDataType.isCompatibleWith(dataType);
    }

    public DataType cloneDataType()
    {
        return theDataType.cloneDataType();
    }

    @Override
    public String toString()
    {
        return theDataType.toString();    
    }

    // These are illegal
    public void setEncoding(String encoding)
    {
        attemptToMutate();
    }

    public void setMimeType(String mimeType)
    {
        attemptToMutate();
    }

    protected DataType<T> getWrappedDataType()
    {
        if (theDataType instanceof ImmutableDataType)
        {
            return ((ImmutableDataType)theDataType).getWrappedDataType();
        }
        else
        {
            return theDataType;
        }
    }

    private void attemptToMutate()
    {
        throw new UnsupportedOperationException("Attempt to change immutable DataType " + theDataType);
    }
    
    @Override
    public int hashCode()
    {
        return theDataType.hashCode();
    }
}
