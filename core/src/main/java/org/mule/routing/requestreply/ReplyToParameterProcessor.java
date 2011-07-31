/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.requestreply;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

/**
 * Propagate any replyTo parameter from the event's message to the event itself.
 */
public class ReplyToParameterProcessor implements MessageProcessor
{
    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        event.captureReplyToDestination();
        return event;
    }
}
