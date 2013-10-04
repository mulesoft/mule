/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
