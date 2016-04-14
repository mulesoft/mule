/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

import java.util.List;

/**
 * Defines an strategy to route a {@link org.mule.api.MuleEvent} through a set of {@link org.mule.api.processor.MessageProcessor}
 */
public interface RoutingStrategy
{

    /**
     * Routes {@link MuleEvent} through a set of {@link MessageProcessor}
     *
     * @param event
     * @param messageProcessors
     * @return
     */
    MuleEvent route(MuleEvent event, List<MessageProcessor> messageProcessors) throws MessagingException;
}
