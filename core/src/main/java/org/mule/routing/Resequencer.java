/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.VoidMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.correlation.CorrelationSequenceComparator;
import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.routing.correlation.ResequenceMessagesCorrelatorCallback;

import java.util.Comparator;

/**
 * <code>Resequencer</code> is used to resequence events according to their dispatch
 * sequence in the correlation group. When the message splitter router splits an
 * event it assigns a correlation sequence to the individual message parts so that
 * another router such as the <i>Resequencer</i> can receive the parts and reorder or
 * merge them.
 * <p>
 * <b>EIP Reference:</b> <a
 * href="http://www.eaipatterns.com/Resequencer.html">http://
 * www.eaipatterns.com/Resequencer.html<a/>
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

    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext)
    {
        return new ResequenceMessagesCorrelatorCallback(getEventComparator(), muleContext, persistentStores,
            storePrefix);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent result = eventCorrelator.process(event);
        if (!isEventValid(result))
        {
            return result;
        }
        MuleEvent last = null;
        for (MuleEvent muleEvent : (MuleEvent[]) result.getMessage().getPayload())
        {
            last = processNext(muleEvent);
        }
        // Respect existing behaviour by returning last event
        return last;
    }

}
