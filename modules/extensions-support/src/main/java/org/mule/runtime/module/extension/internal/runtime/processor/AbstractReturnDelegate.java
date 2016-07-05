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
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
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
        MediaType mediaType = resolveMediaType(value, operationContext);
        if (value instanceof MuleMessage)
        {
            MuleMessage outputMessage = (MuleMessage) value;
            return MuleMessage.builder().payload(outputMessage.getPayload()).mediaType(mediaType)
                    .attributes(resolveAttributes(outputMessage, operationContext)).build();
        }
        else
        {
            return MuleMessage.builder().payload(value).mediaType(mediaType).attributes
                    (operationContext.getEvent().getMessage().getAttributes()).build();
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
    private MediaType resolveMediaType(Object value, OperationContextAdapter operationContext)
    {
        Charset existingEncoding = getDefaultEncoding(muleContext);
        MediaType mediaType;
        if (value instanceof MuleMessage)
        {
            DataType dataType = ((MuleMessage) value).getDataType();
            if (dataType.getMediaType().getCharset().isPresent())
            {
                existingEncoding = dataType.getMediaType().getCharset().get();
            }
            mediaType = dataType.getMediaType();
        }
        else
        {
            mediaType = MediaType.ANY;
        }

        if (operationContext.hasParameter(MIME_TYPE_PARAMETER_NAME))
        {
            mediaType = MediaType.parse(operationContext.getTypeSafeParameter(MIME_TYPE_PARAMETER_NAME, String.class));
        }

        if (operationContext.hasParameter(ENCODING_PARAMETER_NAME))
        {
            mediaType = mediaType.withCharset(Charset.forName(operationContext.getTypeSafeParameter(ENCODING_PARAMETER_NAME, String.class)));
        }
        else
        {
            mediaType = mediaType.withCharset(existingEncoding);
        }

        return mediaType;
    }
}
