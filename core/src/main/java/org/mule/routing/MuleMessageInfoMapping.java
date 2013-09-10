/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.MuleMessage;

/**
 * A simple facade implementation of {@link org.mule.api.routing.MessageInfoMapping} that simply
 * grabs the message information from the {@link org.mule.api.MuleMessage} untouched.
 */
public class MuleMessageInfoMapping implements MessageInfoMapping
{
    public String getCorrelationId(MuleMessage message)
    {
        String id= message.getCorrelationId();
        if (id == null)
        {
            id = getMessageId(message);
        }
        return id;
    }

    public String getMessageId(MuleMessage message)
    {
        return message.getUniqueId();
    }
}
