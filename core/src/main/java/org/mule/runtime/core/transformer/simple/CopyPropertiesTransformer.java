/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.WildcardAttributeEvaluator;

import java.io.Serializable;

public class CopyPropertiesTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator propertyNameEvaluator;
    private WildcardAttributeEvaluator wildcardPropertyNameEvaluator;

    public CopyPropertiesTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        this.propertyNameEvaluator.initialize(muleContext.getExpressionManager());
    }

    @Override
    public Object transformMessage(MuleEvent event, String outputEncoding) throws TransformerException
    {
        MuleMessage message = event.getMessage();
        if (wildcardPropertyNameEvaluator.hasWildcards())
        {
            wildcardPropertyNameEvaluator.processValues(message.getInboundPropertyNames(), matchedValue -> message
                    .copyProperty(matchedValue));
        }
        else
        {
            Object keyValue = propertyNameEvaluator.resolveValue(event);
            if (keyValue != null)
            {
                String propertyName = keyValue.toString();
                Serializable propertyValue = message.getInboundProperty(propertyName);
                if (propertyValue != null)
                {
                    message.copyProperty(propertyName);
                }
                else
                {
                    logger.info("Property value for is null, no property will be copied");
                }
            }
            else
            {
                logger.info("Key expression return null, no property will be copied");
            }
        }
        return message;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        CopyPropertiesTransformer clone = (CopyPropertiesTransformer) super.clone();
        clone.setPropertyName(this.propertyNameEvaluator.getRawValue());
        return clone;
    }

    public void setPropertyName(String propertyName)
    {
        if (propertyName == null)
        {
            throw new IllegalArgumentException("Null propertyName not supported");
        }
        this.propertyNameEvaluator = new AttributeEvaluator(propertyName);
        this.wildcardPropertyNameEvaluator = new WildcardAttributeEvaluator(propertyName);
    }

}
