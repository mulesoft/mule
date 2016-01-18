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
import org.mule.api.MuleEvent;
import org.mule.api.metadata.DataType;
import org.mule.api.temporary.MuleMessage;
import org.mule.extension.api.runtime.ContentMetadata;
import org.mule.extension.api.runtime.ContentType;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.transformer.types.DataTypeFactory;

import java.io.Serializable;

/**
 * An implementation of {@link ReturnDelegate} which allows
 * setting the response value into the {@link MuleMessage} that will
 * continue through the pipeline.
 *
 * @since 4.0
 */
final class ValueReturnDelegate implements ReturnDelegate
{

    private final MuleContext muleContext;

    /**
     * Creates a new instance
     *
     * @param muleContext the {@link MuleContext} of the owning application
     */
    ValueReturnDelegate(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    /**
     * If the {@code value} is a {@link MuleMessage}, then a new one is created merging
     * the contents of the returned value with the ones of the input message. The merging
     * criteria is as follows:
     * <li>
     * <ul>The {@code value}'s payload and DataType is set on the output message</ul>
     * <ul>If the {@code value} has a not {@code null} output for {@link MuleMessage#getAttributes()},
     * then that value is set on the outbound message. Otherwise, whatever value the input message had
     * is maintained</ul>
     * </li>
     * <p>
     * If the {@code value} is of any other type, then it's set as the payload of the outgoing message
     * {@inheritDoc}
     */
    @Override
    public MuleEvent asReturnValue(Object value, OperationContextAdapter operationContext)
    {
        MuleEvent event = operationContext.getEvent();
        if (value instanceof MuleMessage)
        {
            event.setMessage(merge(event.getMessage(), (MuleMessage) value));
        }
        else
        {
            event.getMessage().setPayload(value);
        }

        ContentMetadata contentMetadata = operationContext.getVariable(CONTENT_METADATA);
        if (contentMetadata != null)
        {
            final ContentType contentType = contentMetadata.getOutputContentType();
            final MuleMessage message = event.getMessage();
            final Object payload = message.getPayload();
            final DataType<?> dataType = DataTypeFactory.createWithEncoding(payload.getClass(), contentType.getEncoding().name());
            dataType.setMimeType(contentType.getMimeType());

            ((DefaultMuleMessage) message).setPayload(message.getPayload(), dataType);
        }

        return event;
    }

    private DefaultMuleMessage merge(MuleMessage originalMessage, MuleMessage resultMessage)
    {
        DataType dataType = resultMessage.getDataType();
        if (dataType == null)
        {
            dataType = originalMessage.getDataType();
        }

        Serializable attributes = resultMessage.getAttributes();
        if (attributes == null)
        {
            attributes = originalMessage.getAttributes();
        }

        return new DefaultMuleMessage(resultMessage.getPayload(), dataType, attributes, muleContext);
    }
}
