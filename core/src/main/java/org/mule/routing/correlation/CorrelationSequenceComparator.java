/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.correlation;

import org.mule.api.MuleEvent;

import java.util.Comparator;

/**
 * <code>CorrelationSequenceComparator</code> is a {@link Comparator} for
 * {@link MuleEvent}s using their respective correlation sequences.
 */
public final class CorrelationSequenceComparator implements Comparator<MuleEvent>
{
    public int compare(MuleEvent event1, MuleEvent event2)
    {
        int val1 = event1.getMessage().getCorrelationSequence();
        int val2 = event2.getMessage().getCorrelationSequence();

        if (val1 == val2)
        {
            return 0;
        }
        else if (val1 > val2)
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }
}
