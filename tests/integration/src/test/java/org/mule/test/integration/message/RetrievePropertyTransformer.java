/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.message;

import org.mule.api.MuleMessage;
import org.mule.transformer.AbstractMessageTransformer;

public class RetrievePropertyTransformer extends AbstractMessageTransformer
{
    private String property;
    
    @Override
    public Object transformMessage(MuleMessage message, String encoding)
    {
        Object storedProperty = message.getInboundProperty(property);
        return storedProperty != null ? storedProperty.getClass().getName() : null;
    }

    public String getProperty()
    {
        return property;
    }

    public void setProperty(String property)
    {
        this.property = property;
    }
}
