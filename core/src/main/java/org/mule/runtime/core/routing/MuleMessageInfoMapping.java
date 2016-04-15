/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.routing.MessageInfoMapping;

/**
 * A simple facade implementation of {@link org.mule.runtime.core.api.routing.MessageInfoMapping} that simply
 * grabs the message information from the {@link org.mule.runtime.core.api.MuleMessage} untouched.
 */
public class MuleMessageInfoMapping implements MessageInfoMapping
{
    public String getCorrelationId(MuleEvent event)
    {
        String id= event.getMessage().getCorrelationId();
        if (id == null)
        {
            id = getMessageId(event);
        }
        return id;
    }

    public String getMessageId(MuleEvent event)
    {
        return event.getMessage().getUniqueId();
    }
}
