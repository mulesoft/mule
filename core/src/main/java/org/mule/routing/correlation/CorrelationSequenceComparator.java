/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
