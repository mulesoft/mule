/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.AbstractAnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.metadata.DataType;
import org.mule.api.metadata.SimpleDataType;
import org.mule.transformer.types.TypedValue;
import org.mule.transport.NullPayload;
import org.mule.util.AttributeEvaluator;
import org.mule.util.StringUtils;

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
        if (StringUtils.isEmpty(mimeType) && StringUtils.isEmpty(encoding))
        {
            final TypedValue typedValue = resolveTypedValue(event);
            event.getMessage().setPayload(typedValue.getValue(), typedValue.getDataType());
        }
        else
        {
            Object value = resolveValue(event);
            DataType dataType = resolveDataType(value);

            event.getMessage().setPayload(value, dataType);
        }

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
            return new TypedValue(NullPayload.getInstance(), DataType.OBJECT_DATA_TYPE);
        }
        else
        {
            return valueEvaluator.resolveTypedValue(event);
        }
    }

    private DataType resolveDataType(Object value)
    {
        Class type = (value == null || value instanceof NullPayload) ? Object.class : value.getClass();

        SimpleDataType simpleDataType = new SimpleDataType(type, mimeType);
        simpleDataType.setEncoding(encoding);

        return simpleDataType;
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
