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
    @Override
    public String getCorrelationId(MuleEvent event)
    {
        return event.getMessage().getCorrelation().getId().orElse(getMessageId(event));
    }

    @Override
    public String getMessageId(MuleEvent event)
    {
        return event.getMessage().getUniqueId();
    }
}
