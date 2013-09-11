/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.correlation;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.expression.MessageHeaderExpressionEvaluator;

/**
 * <code>CorrelationPropertiesExpressionEvaluator</code> is a default implementation used for
 * getting the Correlation information from a message. This object is only used when
 * getting a specific property to be set on the message. When reading the property
 * the getProperty(...) or the direct property accessor will be used i.e.
 * message.getCorrelationId() or
 * message.getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY)
 */
public class CorrelationPropertiesExpressionEvaluator extends MessageHeaderExpressionEvaluator
{
    @Override
    public final Object evaluate(String name, MuleMessage message)
    {
        Object result;
        if (message != null)
        {
            if (MuleProperties.MULE_CORRELATION_ID_PROPERTY.equals(name))
            {
                result = getCorrelationId(message);
            }
            else if (MuleProperties.MULE_MESSAGE_ID_PROPERTY.equals(name))
            {
                result = getMessageId(message);
            }
            else
            {
                throw new IllegalArgumentException("Property name: " + name
                                                   + " not recognised by the Correlation Property Extractor");
            }
            if (result == null)
            {
                throw new IllegalArgumentException(
                    "Property Extractor cannot return a null value. Extractor is: " + getClass().getName());
            }
        }
        else
        {
            return super.evaluate(name, message);
        }
        return result;
    }

    public String getMessageId(MuleMessage message)
    {
        return message.getUniqueId();
    }

    public String getCorrelationId(MuleMessage message)
    {
        String id = message.getCorrelationId();
        if (id == null)
        {
            id = message.getUniqueId();
        }
        return id;
    }
}
