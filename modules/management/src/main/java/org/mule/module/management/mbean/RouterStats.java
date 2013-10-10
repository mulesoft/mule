/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.mbean;

import org.mule.management.stats.RouterStatistics;

import java.util.Map;

/**
 * <code>RouterStats</code> TODO
 */
public class RouterStats implements RouterStatsMBean
{

    private RouterStatistics statistics;

    public RouterStats(RouterStatistics statistics)
    {
        this.statistics = statistics;
    }

    public long getCaughtMessages()
    {
        return statistics.getCaughtMessages();
    }

    public long getNotRouted()
    {
        return statistics.getNotRouted();
    }

    public long getTotalReceived()
    {
        return statistics.getTotalReceived();
    }

    public long getTotalRouted()
    {
        return statistics.getTotalRouted();
    }

    public Map getRouted()
    {
        return statistics.getRouted();
    }

}
