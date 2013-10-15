/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.Message;

/**
 * TODO document
 *
 */
public class AggregationException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1276049971165761454L;

    private EventGroup eventGroup = null;

    public AggregationException(EventGroup eventGroup, MessageProcessor endpoint)
    {
        super(eventGroup.getMessageCollectionEvent(), endpoint);
        this.eventGroup = eventGroup;
    }

    public AggregationException(EventGroup eventGroup, MessageProcessor endpoint, Throwable cause)
    {
        super(eventGroup.getMessageCollectionEvent(), endpoint, cause);
        this.eventGroup = eventGroup;
    }

    public AggregationException(Message message, EventGroup eventGroup, MessageProcessor endpoint)
    {
        super(message, eventGroup.getMessageCollectionEvent(), endpoint);
        this.eventGroup = eventGroup;
    }

    public AggregationException(Message message, EventGroup eventGroup, MessageProcessor endpoint,
        Throwable cause)
    {
        super(message, eventGroup.getMessageCollectionEvent(), endpoint, cause);
        this.eventGroup = eventGroup;
    }

    public EventGroup getEventGroup()
    {
        return eventGroup;
    }
}
