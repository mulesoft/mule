/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
