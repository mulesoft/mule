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

import java.util.Comparator;

import org.mule.umo.UMOEvent;

/**
 * <code>CorrelationEventResequencer</code> is used to resequence events
 * according to their dispatch sequence in the correlation group. When the
 * MessageSplitter router splits an event it assigns a correlation sequence to
 * the individual message parts so that another router such as the
 * <i>CorrelationEventResequencer</i> can receive the parts and reorder them or
 * merge them.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CorrelationEventResequencer extends AbstractEventResequencer
{
    public CorrelationEventResequencer()
    {
        setComparator(new CorrelationSequenceComparator());
    }

    protected boolean shouldResequence(EventGroup events)
    {
        UMOEvent event = (UMOEvent) events.getEvents().get(0);
        int size = event.getMessage().getCorrelationGroupSize();
        if (size == -1) {
            logger.warn("Correlation Group Size not set, but CorrelationResequencer is being used.  This can cause messages to be held indefinitely");
        }
        return size == events.getSize();
    }

    private class CorrelationSequenceComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            int val1 = ((UMOEvent) o1).getMessage().getCorrelationSequence();
            int val2 = ((UMOEvent) o2).getMessage().getCorrelationSequence();
            if (val1 == val2) {
                return 0;
            } else if (val1 > val2) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}
