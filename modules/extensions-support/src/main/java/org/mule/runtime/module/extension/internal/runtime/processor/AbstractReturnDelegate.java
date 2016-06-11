/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.processor;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;

import java.io.Serializable;

/**
 * Base class for {@link ReturnDelegate} implementations.
 * <p/>
 * Contains the logic for taking an operation's output value
 * and turn it into a {@link MuleMessage} which not only contains
 * the updated payload but also the proper {@link DataType}
 * and attributes.
 *
 * @since 4.0
 */
abstract class AbstractReturnDelegate implements ReturnDelegate
{

    protected final MuleContext muleContext;

    /**
     * Creates a new instance
     *
     * @param muleContext the {@link MuleContext} of the owning application
     */
    protected AbstractReturnDelegate(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    protected MuleMessage toMessage(Object value, OperationContextAdapter operationContext)
    {
        DataType dataType = resolveDataType(value, operationContext);
        if (value instanceof MuleMessage)
        {
            MuleMessage outputMessage = (MuleMessage) value;
            return new DefaultMuleMessage(outputMessage.getPayload(), dataType, resolveAttributes(outputMessage, operationContext), muleContext);
        }
        else
        {
            return new DefaultMuleMessage(value, dataType, operationContext.getEvent().getMessage().getAttributes(), muleContext);
        }
    }

    private Serializable resolveAttributes(MuleMessage outputMessage, OperationContextAdapter operationContext)
    {
        Serializable attributes = outputMessage.getAttributes();
        return attributes != null ? attributes : operationContext.getEvent().getMessage().getAttributes();
    }

    private DataType resolveDataType(Object value, OperationContextAdapter operationContext)
    {
        String mimeType = operationContext.getTypeSafeParameter(MIME_TYPE_PARAMETER_NAME, String.class);
        String encoding = operationContext.getTypeSafeParameter(ENCODING_PARAMETER_NAME, String.class);
        DataType dataType = null;
        Class<?> type = value != null ? value.getClass() : NullPayload.class;

        if (value instanceof MuleMessage)
        {
            dataType = ((MuleMessage) value).getDataType();
            type = dataType.getType();
            if (encoding == null && mimeType == null)
            {
                return dataType;
            }

            if (encoding == null)
            {
                encoding = dataType.getEncoding();
            }

            if (mimeType == null)
            {
                mimeType = dataType.getMimeType();
            }
        }

        if (dataType != null && encoding == null && mimeType == null)
        {
            return dataType;
        }

        if (value == null || value instanceof NullPayload)
        {
            return null;
        }

        if (encoding == null)
        {
            encoding = muleContext.getConfiguration().getDefaultEncoding();
        }

        return DataType.builder(value.getClass()).mimeType(mimeType).encoding(encoding).build();
    }
}
