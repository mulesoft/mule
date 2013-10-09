/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
