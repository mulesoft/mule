/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;


import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.util.AttributeEvaluator;

/**
 * Transformer that modifies the payload of the message according to the provided value.
 */
public class SetPayloadTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator valueEvaluator;

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        valueEvaluator.initialize(muleContext.getExpressionManager());
    }

    public SetPayloadTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        if(valueEvaluator.getRawValue() == null)
        {
            return NullPayload.getInstance();
        }

        return valueEvaluator.resolveValue(message);
    }

    public void setValue(String value)
    {
        valueEvaluator = new AttributeEvaluator(value);
    }
}
