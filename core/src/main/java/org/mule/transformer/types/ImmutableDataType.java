/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
