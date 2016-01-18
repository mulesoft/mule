/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import org.mule.api.MuleEvent;
import org.mule.transformer.AbstractMessageTransformer;

public class RetrievePropertyTransformer extends AbstractMessageTransformer
{
    private String property;
    
    @Override
    public Object transformMessage(MuleEvent event, String encoding)
    {
        Object storedProperty = event.getMessage().getOutboundProperty(property);
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
