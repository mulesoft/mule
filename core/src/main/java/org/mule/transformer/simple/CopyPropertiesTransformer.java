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
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;
import org.mule.util.WildcardAttributeEvaluator;

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
    public Object transformMessage(final MuleMessage message, String outputEncoding) throws TransformerException
    {
        if (wildcardPropertyNameEvaluator.hasWildcards())
        {
            wildcardPropertyNameEvaluator.processValues(message.getInboundPropertyNames(), new WildcardAttributeEvaluator.MatchCallback()
            {
                @Override
                public void processMatch(String matchedValue)
                {
                    message.setOutboundProperty(matchedValue, message.getInboundProperty(matchedValue), message.getPropertyDataType(matchedValue, PropertyScope.INBOUND));
                }
            });
        }
        else
        {
            Object keyValue = propertyNameEvaluator.resolveValue(message);
            if (keyValue != null)
            {
                String propertyName = keyValue.toString();
                Object propertyValue = message.getInboundProperty(propertyName);
                if (propertyValue != null)
                {
                    message.setOutboundProperty(propertyName, propertyValue, message.getPropertyDataType(propertyName, PropertyScope.INBOUND));
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
