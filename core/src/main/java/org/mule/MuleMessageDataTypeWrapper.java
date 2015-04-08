/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.DataType;
import org.mule.transformer.types.MimeTypes;

/**
 * Wraps the @{link DataType} associated to a {@link MuleMessage} in order to
 * update message properties when data type is modified and vice versa.
 *
 * @since 3.7.0
 */
class MuleMessageDataTypeWrapper<T> implements DataType<T>
{

    private final DefaultMuleMessage muleMessage;
    private final DataType dataType;

    public MuleMessageDataTypeWrapper(DefaultMuleMessage muleMessage, DataType dataType)
    {
        this.muleMessage = muleMessage;
        this.dataType = dataType;
    }

    @Override
    public Class<?> getType()
    {
        return dataType.getType();
    }

    @Override
    public String getMimeType()
    {
        return dataType.getMimeType();
    }

    @Override
    public String getEncoding()
    {
        return dataType.getEncoding();
    }

    @Override
    public void setEncoding(String encoding)
    {
        dataType.setEncoding(encoding);

        if (encoding != null)
        {
            muleMessage.setOutboundProperty(MuleProperties.MULE_ENCODING_PROPERTY, encoding);
        }
    }

    @Override
    public void setMimeType(String mimeType)
    {
        dataType.setMimeType(mimeType);

        if (mimeType != null && !mimeType.equals(MimeTypes.ANY))
        {
            String encoding = getEncoding();
            if (encoding != null)
            {
                mimeType = mimeType + ";charset=" + encoding;
            }

            muleMessage.setOutboundProperty(MuleProperties.CONTENT_TYPE_PROPERTY, mimeType);
        }
    }

    @Override
    public boolean isCompatibleWith(DataType dataType)
    {
        return dataType.isCompatibleWith(dataType);
    }

    @Override
    public DataType cloneDataType()
    {
        return dataType.cloneDataType();
    }

    public DataType<?> getDelegate()
    {
        return dataType;
    }

    @Override
    public String toString()
    {
        return "MuleMessageDataTypeWrapper{" + dataType.toString() + "}";
    }
}
