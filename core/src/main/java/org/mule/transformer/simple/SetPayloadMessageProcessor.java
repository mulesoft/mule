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
import org.mule.api.transformer.DataType;
import org.mule.transformer.types.SimpleDataType;
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
            DataType dataType = resolveDataType(event, value);

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
            value = valueEvaluator.resolveValue(event.getMessage());
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
            return valueEvaluator.resolveTypedValue(event.getMessage());
        }
    }

    private DataType resolveDataType(MuleEvent event, Object value)
    {
        Class type = (value == null || value instanceof NullPayload) ? Object.class : value.getClass();

        SimpleDataType simpleDataType = new SimpleDataType(type, mimeType);
        simpleDataType.setEncoding(encoding);

        return simpleDataType;
    }

    /**
     * Sets the name of the message processor
     *
     * @param name the name of the message processor
     * @Deprecate this setter is provided for backwards compatibility to enable
     * global message processor definition in the mule configuration
     * as {@link SetPayloadTransformer}
     */
    @Deprecated
    public void setName(String name)
    {
        // Do nothing
    }

    /**
     * @Deprecate this setter is provided for backwards compatibility at mule
     * configuration level with {@link SetPayloadTransformer}
     */
    @Deprecated
    public void setIgnoreBadInput(boolean ignoreBadInput)
    {
        // Do nothing
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    /**
     * @Deprecate this setter is provided for backwards compatibility at mule
     * configuration level with {@link SetPayloadTransformer}
     */
    public void setReturnClass(String className)
    {
        // Do nothing
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
