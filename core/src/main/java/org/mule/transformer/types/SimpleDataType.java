/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.types;

import org.mule.api.transformer.DataType;
import org.mule.util.StringUtils;

import java.nio.charset.Charset;

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

    private static final String CHARSET_PARAM = "charset";

    protected final Class<?> type;
    protected String mimeType = ANY_MIME_TYPE;
    protected String encoding;

    public SimpleDataType(Class<?> type, String mimeType)
    {
        this.type = type;
        setMimeType(mimeType);
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
                if (mt.getParameter(CHARSET_PARAM) != null)
                {
                    setEncoding(mt.getParameter(CHARSET_PARAM));
                }
            }
            catch (MimeTypeParseException e)
            {
                throw new IllegalArgumentException("MimeType cannot be parsed :" + mimeType);
            }
        }
    }

    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        if (!StringUtils.isEmpty(encoding))
        {
            // Checks that the encoding is valid and supported
            Charset.forName(encoding);
        }

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
                "type=" + (type == null ? null : type.getName())+
                ", mimeType='" + mimeType + '\'' +
                ", encoding='" + encoding + '\'' +
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
