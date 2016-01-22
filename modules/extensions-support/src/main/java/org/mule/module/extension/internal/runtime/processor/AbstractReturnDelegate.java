/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.processor;

import static org.mule.module.extension.internal.ExtensionProperties.CONTENT_METADATA;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.metadata.DataType;
import org.mule.api.temporary.MuleMessage;
import org.mule.extension.api.runtime.ContentMetadata;
import org.mule.extension.api.runtime.ContentType;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;

import java.io.Serializable;

/**
 * Base class for {@link ReturnDelegate} implementations.
 * <p>
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
        ContentMetadata contentMetadata = operationContext.getVariable(CONTENT_METADATA);
        if (contentMetadata == null)
        {
            if (value instanceof MuleMessage)
            {
                return ((MuleMessage) value).getDataType();
            }

            return null;
        }

        if (value == null || value instanceof NullPayload)
        {
            return null;
        }

        final ContentType contentType = contentMetadata.getOutputContentType();

        final DataType dataType = DataTypeFactory.createWithEncoding(value.getClass(), contentType.getEncoding().name());
        dataType.setMimeType(contentType.getMimeType());

        return dataType;
    }
}
