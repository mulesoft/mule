/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.message;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class RetrievePropertyTransformer extends AbstractMessageAwareTransformer
{
    private String property;
    
    @Override
    public Object transform(MuleMessage message, String encoding) throws TransformerException
    {
        Object storedProperty = message.getProperty(property);
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
