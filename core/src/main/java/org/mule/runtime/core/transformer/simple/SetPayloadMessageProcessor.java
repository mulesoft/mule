/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.transformer.simple;

import static org.apache.commons.lang.StringUtils.isEmpty;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
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

    private String mimeType;
    private String encoding;
    private AttributeEvaluator valueEvaluator = new AttributeEvaluator(null);
    private MuleContext muleContext;


    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        event.setMessage(event.getMessage().transform(msg -> {
            if (isEmpty(mimeType) && isEmpty(encoding))
            {
                final TypedValue typedValue = resolveTypedValue(event);
                msg.setPayload(typedValue.getValue(), typedValue.getDataType());
            }
            else
            {
                Object value = resolveValue(event);
                DataType dataType = resolveDataType(value);
                msg.setPayload(value, dataType);
            }
            return msg;
        }));
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

    private DataType<?> resolveDataType(Object value)
    {
        Class<?> type = (value == null || value instanceof NullPayload) ? Object.class : value.getClass();

        return DataType.builder(type).mimeType(mimeType).encoding(encoding).build();
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
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
