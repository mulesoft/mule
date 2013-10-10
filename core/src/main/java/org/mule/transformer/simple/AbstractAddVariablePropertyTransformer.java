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
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;

import java.text.MessageFormat;

public abstract class AbstractAddVariablePropertyTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator identifierEvaluator;
    private AttributeEvaluator valueEvaluator;

    public AbstractAddVariablePropertyTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        identifierEvaluator.initialize(muleContext.getExpressionManager());
        valueEvaluator.initialize(muleContext.getExpressionManager());
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Object keyValue = identifierEvaluator.resolveValue(message);
        String key = (keyValue == null ? null : keyValue.toString());
        if (key == null)
        {
            logger.error("Setting Null variable keys is not supported, this entry is being ignored");
        }
        else
        {
            Object value = valueEvaluator.resolveValue(message);
            if (value == null)
            {
                logger.info(MessageFormat.format(
                        "Variable with key \"{0}\", not found on message using \"{1}\". Since the value was marked optional, nothing was set on the message for this variable",
                        key, valueEvaluator.getRawValue()));
            }
            else
            {
                message.setProperty(key, value, getScope());
            }
        }
        return message;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        AbstractAddVariablePropertyTransformer clone = (AbstractAddVariablePropertyTransformer) super.clone();
        clone.setIdentifier(this.identifierEvaluator.getRawValue());
        clone.setValue(this.valueEvaluator.getRawValue());
        return clone;
    }

    public void setIdentifier(String identifier)
    {
        if (identifier == null)
        {
            throw new IllegalArgumentException("Key must not be null");
        }
        this.identifierEvaluator = new AttributeEvaluator(identifier);
    }

    public void setValue(String value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("Value must not be null");
        }
        this.valueEvaluator = new AttributeEvaluator(value);
    }

    abstract protected PropertyScope getScope();

}
