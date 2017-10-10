/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.cron;

import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.ApplicationContextBuilder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

public class CronsTimeZoneSchedulerTest {
    final static String TIMEZONE_XML = "America/Argentina/Buenos_Aires";



    @Test
    public void validTimeZoneInScheduler() throws Exception {
        ApplicationContextBuilder builder = new ApplicationContextBuilder();
        builder.setApplicationResources(new String[]{"cron-scheduler-valid-time-zone.xml"});
        MuleContext test = builder.build();
        assertThat(test.getRegistry().lookupByType(CronSchedulerFactory.class).size(), equalTo(1));
        assertThat(test.getRegistry().lookupByType(CronScheduler.class).values().iterator().next().getTimeZone().getID(), equalTo(TIMEZONE_XML));
    }

    @Test
    public void invalidTimeZoneInScheduler() throws Exception {
        try
        {
            ApplicationContextBuilder builder = new ApplicationContextBuilder();
            builder.setApplicationResources(new String[]{"cron-timezone-scheduler-config.xml"});
            MuleContext test = builder.build();

        } catch (Exception e)
        {
            assertThat(getRootCause(e).getMessage(), equalTo("Invalid Timezone"));
        }

    }
}
