/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;
import org.mule.transport.NullPayload;
import org.mule.util.AttributeEvaluator;
import org.mule.util.StringUtils;

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
            TypedValue typedValue = valueEvaluator.resolveTypedValue(message);
            if (typedValue.getValue() == null || typedValue.getValue() instanceof NullPayload)
            {
                message.removeProperty(key, getScope());

                if (logger.isDebugEnabled())
                {
                    logger.debug(MessageFormat.format(
                            "Variable with key \"{0}\", not found on message using \"{1}\". Since the value was marked optional, nothing was set on the message for this variable",
                            key, valueEvaluator.getRawValue()));
                }
            }
            else
            {
                if (!StringUtils.isEmpty(mimeType) || !StringUtils.isEmpty(encoding))
                {
                    DataType<?> dataType = DataTypeFactory.create(typedValue.getValue().getClass(), getMimeType());
                    dataType.setEncoding(getEncoding());
                    message.setProperty(key, typedValue.getValue(), getScope(), dataType);
                }
                else
                {
                    message.setProperty(key, typedValue.getValue(), getScope(), typedValue.getDataType());
                }
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
        if (StringUtils.isBlank(identifier))
        {
            throw new IllegalArgumentException("Key cannot be blank");
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
