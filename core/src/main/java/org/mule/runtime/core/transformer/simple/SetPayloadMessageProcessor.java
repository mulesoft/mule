/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.transformer.simple;

import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.AttributeEvaluator;

/**
 * Modifies the payload of a {@link MuleMessage} according to the provided value.
 */
public class SetPayloadMessageProcessor extends AbstractAnnotatedObject implements MessageProcessor, MuleContextAware, Initialisable
{

    private DataType dataType;
    private AttributeEvaluator valueEvaluator = new AttributeEvaluator(null);
    private MuleContext muleContext;


    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        final Builder builder = MuleMessage.builder(event.getMessage());

        if (dataType == null)
        {
            final TypedValue typedValue = resolveTypedValue(event);
            if (typedValue.getDataType().getMediaType().getCharset().isPresent())
            {
                builder.payload(typedValue.getValue()).mediaType(typedValue.getDataType().getMediaType());
            }
            else
            {
                builder.payload(typedValue.getValue()).mediaType(DataType.builder(typedValue.getDataType())
                                                                         .charset(getDefaultEncoding(muleContext))
                                                                         .build()
                                                                         .getMediaType());
            }
        }
        else
        {
            Object value = resolveValue(event);
            final DataTypeParamsBuilder dataTypeBuilder = DataType.builder(dataType).type((value == null || value instanceof NullPayload) ? Object.class : value.getClass());
            if (dataType.getMediaType().getCharset().isPresent())
            {
                builder.payload(value).mediaType(dataTypeBuilder.build().getMediaType());
            }
            else
            {
                builder.payload(value).mediaType(dataTypeBuilder.charset(getDefaultEncoding(muleContext)).build().getMediaType());
            }
        }

        event.setMessage(builder.build());
        return event;
    }

    private Object resolveValue(MuleEvent event)
    {
        Object value;
        if (valueEvaluator.getRawValue() == null)
        {
            value = NullPayload.getInstance();
        }
        else
        {
            value = valueEvaluator.resolveValue(event);
        }
        return value;
    }

    private TypedValue resolveTypedValue(MuleEvent event)
    {
        if (valueEvaluator.getRawValue() == null)
        {
            return new TypedValue(NullPayload.getInstance(), DataType.OBJECT);
        }
        else
        {
            return valueEvaluator.resolveTypedValue(event);
        }
    }

    public void setMimeType(String mimeType)
    {
        setDataType(DataType.builder(dataType == null ? DataType.OBJECT : dataType).mediaType(mimeType).build());
    }

    public void setEncoding(String encoding)
    {
        setDataType(DataType.builder(dataType == null ? DataType.OBJECT : dataType).charset(encoding).build());
    }

    public void setDataType(DataType dataType)
    {
        if (dataType.getMediaType().getCharset().isPresent())
        {
            this.dataType = dataType;
        }
        else
        {
            this.dataType = DataType.builder(dataType).build();
        }
    }

    public void setValue(String value)
    {
        valueEvaluator = new AttributeEvaluator(value);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        valueEvaluator.initialize(muleContext.getExpressionManager());
    }
}
