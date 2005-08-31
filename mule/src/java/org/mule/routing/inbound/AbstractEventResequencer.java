/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.inbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;

import java.util.*;

/**
 * <code>AbstractEventResequencer</code> is used to receive a set of events,
 * resequence them and forward them on to their destination
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractEventResequencer extends SelectiveConsumer
{
    protected static final String NO_CORRELATION_ID = "no-id";

    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(AbstractEventResequencer.class);

    private Comparator comparator;
    private Map eventGroups = new HashMap();

    public UMOEvent[] process(UMOEvent event) throws MessagingException
    {
        if (isMatch(event)) {
            EventGroup eg = addEvent(event);
            if (shouldResequence(eg)) {
                List resequencedEvents = resequenceEvents(eg);
                removeGroup(eg.getGroupId());
                UMOEvent[] returnEvents = new UMOEvent[resequencedEvents.size()];
                resequencedEvents.toArray(returnEvents);
                return returnEvents;
            }
        }
        return null;
    }

    protected EventGroup addEvent(UMOEvent event)
    {
        String cId = event.getMessage().getCorrelationId();
        if (cId == null) {
            cId = NO_CORRELATION_ID;
        }
        EventGroup eg = (EventGroup) eventGroups.get(cId);
        if (eg == null) {
            eg = new EventGroup(cId);
            eg.addEvent(event);
            eventGroups.put(eg.getGroupId(), eg);
        } else {
            eg.addEvent(event);
        }
        return eg;
    }

    protected void removeGroup(Object id)
    {
        eventGroups.remove(id);
    }

    protected List resequenceEvents(EventGroup events)
    {
        List result = new ArrayList(events.getEvents());
        if (comparator != null) {
            Collections.sort(result, comparator);
        } else {
            logger.warn("Event comparator is null, events were not reordered");
        }
        return result;
    }

    public Comparator getComparator()
    {
        return comparator;
    }

    public void setComparator(Comparator comparator)
    {
        this.comparator = comparator;
    }

    protected abstract boolean shouldResequence(EventGroup events);
}
