/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.types;

import org.mule.api.MuleRuntimeException;
import org.mule.api.transformer.DataType;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.beanutils.MethodUtils;

/**
 * A data type that simply wraps a Java type.  This type also allows a mime type to be associated
 * with the Java type.
 *
 * @since 3.0
 */
public class SimpleDataType<T> implements DataType<T>, Cloneable
{
    protected Class<?> type;
    protected String mimeType = ANY_MIME_TYPE;
    protected String encoding;

    public SimpleDataType(Class<?> type, String mimeType)
    {
        this.type = type;
        if (mimeType == null)
        {
            this.mimeType = ANY_MIME_TYPE;
        }
        else
        {
            try
            {
                MimeType mt = new MimeType(mimeType);
                this.mimeType = mt.getPrimaryType() + "/" + mt.getSubType();
                if (mt.getParameter("charset") != null)
                {
                    encoding = mt.getParameter("charset");
                }
            }
            catch (MimeTypeParseException e)
            {
                //TODO, this should really get thrown
                throw new MuleRuntimeException(e);
            }
        }
    }

    public SimpleDataType(Class type)
    {
        this.type = type;
    }

    public Class getType()
    {
        return type;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = (mimeType == null ? ANY_MIME_TYPE : mimeType);
    }

    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public boolean isCompatibleWith(DataType dataType)
    {
        if (dataType instanceof ImmutableDataType)
        {
            dataType = ((ImmutableDataType) dataType).getWrappedDataType();
        }
        if (this == dataType)
        {
            return true;
        }
        if (dataType == null)
        {
            return false;
        }

        SimpleDataType that = (SimpleDataType) dataType;

        //ANY_MIME_TYPE will match to a null or non-null value for MimeType        
        if ((this.getMimeType() == null && that.getMimeType() != null || that.getMimeType() == null && this.getMimeType() != null) && !ANY_MIME_TYPE.equals(this.mimeType) && !ANY_MIME_TYPE.equals(that.mimeType))
        {
            return false;
        }

        if (this.getMimeType() != null && !this.getMimeType().equals(that.getMimeType()) && !ANY_MIME_TYPE.equals(that.getMimeType()) && !ANY_MIME_TYPE.equals(this.getMimeType()))
        {
            return false;
        }

        if (!fromPrimitive(this.getType()).isAssignableFrom(fromPrimitive(that.getType())))
        {
            return false;
        }

        return true;
    }
    
    
    private Class<?> fromPrimitive(Class<?> type)
    {
        Class<?> primitiveWrapper = MethodUtils.getPrimitiveWrapper(type);
        if (primitiveWrapper != null)
        {
            return primitiveWrapper;
        }
        else
        {
            return type;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        SimpleDataType that = (SimpleDataType) o;

        if (!type.equals(that.type))
        {
            return false;
        }

        //ANY_MIME_TYPE will match to a null or non-null value for MimeType
        if ((this.mimeType == null && that.mimeType != null || that.mimeType == null && this.mimeType != null) && !ANY_MIME_TYPE.equals(that.mimeType))
        {
            return false;
        }

        if (this.mimeType != null && !mimeType.equals(that.mimeType) && !ANY_MIME_TYPE.equals(that.mimeType))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = type.hashCode();
        result = 31 * result + (encoding != null ? encoding.hashCode() : 0);
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        return result;
    }

    
    @Override
    public String toString()
    {
        return "SimpleDataType{" +
                "type=" + type.getName() +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }

    public DataType cloneDataType()
    {
        try
        {
            return (DataType) clone();
        }
        catch (CloneNotSupportedException e)
        {
            // This cannot happen, because we implement Cloneable
            throw new IllegalStateException(e);
        }
    }
}
