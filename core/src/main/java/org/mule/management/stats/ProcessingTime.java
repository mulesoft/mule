/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.stats;

import java.io.Serializable;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

/**
 * Accumulates the processing time for all branches of a flow
 */
public class ProcessingTime implements Serializable
{
    private AtomicLong accumulator = new AtomicLong();

    /**
     * Record the time it took one branch to complete its processing
     */
    public long recordExecutionBranchTime(long time)
    {
        return accumulator.addAndGet(getEffectiveTime(time));
    }

    /**
     * Convert processing time to effective processing time.  If processing took less than a tick, we consider
     * it to have been one millisecond
     */
     public static long getEffectiveTime(long time)
    {
        return (time <= 0) ? 1L : time;
    }

}
