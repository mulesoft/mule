/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.processor.AbstractAnnotatedMessageProcessor;
import org.mule.transformer.types.SimpleDataType;
import org.mule.transport.NullPayload;
import org.mule.util.AttributeEvaluator;

/**
 * Modifies the payload of a {@link MuleMessage} according to the provided value.
 */
public class SetPayloadMessageProcessor extends AbstractAnnotatedMessageProcessor implements MuleContextAware, Initialisable
{

    private String mimeType;
    private String encoding;
    private AttributeEvaluator valueEvaluator;
    private MuleContext muleContext;
    private Class<?> returnClass;


    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
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

        DataType dataType = createDataType(value);

        event.getMessage().setPayload(value, dataType);

        return event;
    }

    private DataType createDataType(Object value)
    {
        Class type;
        if (returnClass != null)
        {
            type = returnClass;
        }
        else
        {
            type = (value == null || value instanceof NullPayload) ? Object.class : value.getClass();
        }

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

    public void setReturnClass(Class<?> returnClass)
    {
        this.returnClass = returnClass;
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
