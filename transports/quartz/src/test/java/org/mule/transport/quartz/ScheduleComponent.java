/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz;

import org.mule.api.annotations.Schedule;

public class ScheduleComponent
{
    @Schedule(interval = 1000)
    public String pingMe()
    {
        return "pinged! (interval)";
    }

    @Schedule(cron = "* * * * * ?")
    public String pingMeCron()
    {
        return "pinged! (cron)";
    }
}
