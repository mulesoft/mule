/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.EventCorrelatorCallback;
import org.mule.routing.ResequenceCorrelatorCallback;

import java.util.Comparator;

/**
 * <code>CorrelationEventResequencer</code> is used to resequence events according
 * to their dispatch sequence in the correlation group. When the MessageSplitter
 * router splits an event it assigns a correlation sequence to the individual message
 * parts so that another router such as the <i>CorrelationEventResequencer</i> can
 * receive the parts and reorder or merge them.
 */
public class CorrelationEventResequencer extends AbstractEventAggregator
{
    protected Comparator eventComparator;

    public CorrelationEventResequencer()
    {
        super();
        this.setEventComparator(new CorrelationSequenceComparator());
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (eventComparator == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("eventComparator"), this);
        }
        super.initialise();

    }

    public Comparator getEventComparator()
    {
        return eventComparator;
    }

    public void setEventComparator(Comparator eventComparator)
    {
        this.eventComparator = eventComparator;
    }

    protected EventCorrelatorCallback getCorrelatorCallback()
    {
        return new ResequenceCorrelatorCallback(getEventComparator());
    }

    @Override
    public MuleEvent[] process(MuleEvent event) throws MessagingException
    {
        MuleMessage msg = eventCorrelator.process(event);
        if (msg == null)
        {
            return null;
        }
        return (MuleEvent[]) msg.getPayload();
    }
}
