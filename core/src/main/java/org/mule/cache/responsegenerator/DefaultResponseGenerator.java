/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.cache.responsegenerator;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;

/**
 * Implements {@link ResponseGenerator} creating a new {@link MuleEvent}
 * for each request.
 */
public class DefaultResponseGenerator implements ResponseGenerator
{

    public MuleEvent create(MuleEvent requestedEvent, MuleEvent cachedResponseEvent)
    {
        MuleMessage clonedMessage = new DefaultMuleMessage(cachedResponseEvent.getMessage());

        return new DefaultMuleEvent(clonedMessage, requestedEvent, requestedEvent.getSession());
    }
}

