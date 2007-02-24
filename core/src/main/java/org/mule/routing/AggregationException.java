/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.providers.NullPayload;
import org.mule.routing.inbound.EventGroup;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.RoutingException;

/**
 * TODO document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AggregationException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1276049971165761454L;

    private EventGroup eventGroup = null;

    public AggregationException(EventGroup eventGroup, UMOImmutableEndpoint endpoint)
    {
        super(new MuleMessage(NullPayload.getInstance()), endpoint);
        this.eventGroup = eventGroup;
    }

    public AggregationException(EventGroup eventGroup, UMOImmutableEndpoint endpoint, Throwable cause)
    {
        super(new MuleMessage(NullPayload.getInstance()), endpoint, cause);
        this.eventGroup = eventGroup;
    }

    public AggregationException(Message message, EventGroup eventGroup, UMOImmutableEndpoint endpoint)
    {
        super(message, new MuleMessage(NullPayload.getInstance()), endpoint);
        this.eventGroup = eventGroup;
    }

    public AggregationException(Message message,
                                EventGroup eventGroup,
                                UMOImmutableEndpoint endpoint,
                                Throwable cause)
    {
        super(message, new MuleMessage(NullPayload.getInstance()), endpoint, cause);
        this.eventGroup = eventGroup;
    }

    public EventGroup getEventGroup()
    {
        return eventGroup;
    }
}
