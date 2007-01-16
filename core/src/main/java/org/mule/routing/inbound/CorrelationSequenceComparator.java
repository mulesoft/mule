/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.umo.UMOEvent;

import java.util.Comparator;

/**
 * <code>CorrelationSequenceComparator</code> is a {@link Comparator} for
 * {@link UMOEvent}s using their respective correlation sequences.
 */
public class CorrelationSequenceComparator implements Comparator
{
    private static final Comparator Instance = new CorrelationSequenceComparator();

    public static Comparator getInstance()
    {
        return Instance;
    }

    private CorrelationSequenceComparator()
    {
        super();
    }

    public int compare(Object o1, Object o2)
    {
        int val1 = ((UMOEvent)o1).getMessage().getCorrelationSequence();
        int val2 = ((UMOEvent)o2).getMessage().getCorrelationSequence();

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
