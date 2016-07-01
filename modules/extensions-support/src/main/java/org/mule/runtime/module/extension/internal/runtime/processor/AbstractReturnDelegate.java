/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.processor;

import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;

import java.io.Serializable;
import java.nio.charset.Charset;

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
            return new DefaultMuleMessage(outputMessage.getPayload(), dataType, resolveAttributes(outputMessage, operationContext));
        }
        else
        {
            return new DefaultMuleMessage(value, dataType, operationContext.getEvent().getMessage().getAttributes());
        }
    }

    private Serializable resolveAttributes(MuleMessage outputMessage, OperationContextAdapter operationContext)
    {
        Serializable attributes = outputMessage.getAttributes();
        return attributes != null ? attributes : operationContext.getEvent().getMessage().getAttributes();
    }

    /**
     * If provided, mimeType and encoding configured as operation parameters will take precedence
     * over what comes with the message's {@link DataType}.
     * 
     * @param value
     * @param operationContext
     * @return
     */
    private DataType resolveDataType(Object value, OperationContextAdapter operationContext)
    {
        if (value == null || value instanceof NullPayload)
        {
            return null;
        }

        Charset existingEncoding = getDefaultEncoding(muleContext);
        DataTypeParamsBuilder dataTypeBuilder;
        if (value instanceof MuleMessage)
        {
            DataType dataType = ((MuleMessage) value).getDataType();
            if (dataType.getMediaType().getCharset().isPresent())
            {
                existingEncoding = dataType.getMediaType().getCharset().get();
            }
            dataTypeBuilder = DataType.builder(dataType);
        }
        else
        {
            dataTypeBuilder = DataType.builder().type((Class) (value != null ? value.getClass() : Object.class));
        }

        if (operationContext.hasParameter(MIME_TYPE_PARAMETER_NAME))
        {
            dataTypeBuilder = dataTypeBuilder.mediaType(operationContext.getTypeSafeParameter(MIME_TYPE_PARAMETER_NAME, String.class));
        }

        if (operationContext.hasParameter(ENCODING_PARAMETER_NAME))
        {
            dataTypeBuilder = dataTypeBuilder.charset(operationContext.getTypeSafeParameter(ENCODING_PARAMETER_NAME, String.class));
        }
        else
        {
            dataTypeBuilder = dataTypeBuilder.charset(existingEncoding);
        }

        return dataTypeBuilder.build();
    }
}
