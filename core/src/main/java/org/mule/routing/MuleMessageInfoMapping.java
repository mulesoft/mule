/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
