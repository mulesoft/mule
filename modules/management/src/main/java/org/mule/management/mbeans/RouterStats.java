/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import org.mule.management.stats.RouterStatistics;

import java.util.Map;

/**
 * <code>RouterStats</code> TODO
 * 
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class RouterStats implements RouterStatsMBean
{

    private RouterStatistics statistics;

    public RouterStats(RouterStatistics statistics)
    {
        this.statistics = statistics;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.management.mbeans.RouterStatsMBean#getCaughtMessages()
     */
    public long getCaughtMessages()
    {
        return statistics.getCaughtMessages();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.management.mbeans.RouterStatsMBean#getNotRouted()
     */
    public long getNotRouted()
    {
        return statistics.getNotRouted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.management.mbeans.RouterStatsMBean#getTotalReceived()
     */
    public long getTotalReceived()
    {
        return statistics.getTotalReceived();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.management.mbeans.RouterStatsMBean#getTotalRouted()
     */
    public long getTotalRouted()
    {
        return statistics.getTotalRouted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.management.mbeans.RouterStatsMBean#getRouted()
     */
    public Map getRouted()
    {
        return statistics.getRouted();
    }

}
