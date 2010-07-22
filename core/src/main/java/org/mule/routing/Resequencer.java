/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.correlation.CorrelationSequenceComparator;
import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.routing.correlation.ResequenceMessagesCorrelatorCallback;

import java.util.Comparator;

/**
 * <code>CorrelationEventResequencer</code> is used to resequence events according
 * to their dispatch sequence in the correlation group. When the MessageSplitter
 * router splits an event it assigns a correlation sequence to the individual message
 * parts so that another router such as the <i>CorrelationEventResequencer</i> can
 * receive the parts and reorder or merge them.
 */
public class Resequencer extends AbstractAggregator
{
    protected Comparator eventComparator;

    public Resequencer()
    {
        super();
        this.setEventComparator(new CorrelationSequenceComparator());
    }

    @Override
    public void ensureInitialised(MuleEvent event) throws MuleException
    {
        if (eventComparator == null)
        {
            throw new DefaultMuleException(CoreMessages.objectIsNull("eventComparator"));
        }
        super.ensureInitialised(event);
    }

    public Comparator getEventComparator()
    {
        return eventComparator;
    }

    public void setEventComparator(Comparator eventComparator)
    {
        this.eventComparator = eventComparator;
    }

    protected EventCorrelatorCallback getCorrelatorCallback(MuleEvent event)
    {
        return new ResequenceMessagesCorrelatorCallback(getEventComparator(), event.getMuleContext());
    }
    
    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        ensureInitialised(event);
        MuleMessage msg = eventCorrelator.process(event);
        if (msg == null)
        {
            return null;
        }
        MuleEvent last = null;
        for (MuleEvent muleEvent : (MuleEvent[]) msg.getPayload())
        {
            last = processNext(muleEvent);
        }
        // Respect existing bahaviour by returning last event
        return last;
    }

}