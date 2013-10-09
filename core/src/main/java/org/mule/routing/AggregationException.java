/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
