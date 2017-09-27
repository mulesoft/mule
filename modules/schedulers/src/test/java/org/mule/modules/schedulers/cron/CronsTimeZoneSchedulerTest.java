/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.cron;

import com.sun.xml.internal.ws.api.model.ExceptionType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.api.MuleContext;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.schedule.SchedulerCreationException;
import org.mule.tck.junit4.ApplicationContextBuilder;
import org.omg.CORBA.DynAnyPackage.Invalid;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CronsTimeZoneSchedulerTest {
    final static String TIMEZONE_XML = "America/Argentina/Buenos_Aires";



    @Test
    public void validTimeZoneInScheduler() throws Exception {
        ApplicationContextBuilder builder = new ApplicationContextBuilder();
        builder.setApplicationResources(new String[]{"cron-scheduler-valid-time-zone.xml"});
        builder.build();
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
            assertThat(e.getCause().getMessage(), equalTo("Invalid Timezone"));
            assertThat(e.getCause().getCause().getCause().getClass().getName(), equalTo("org.mule.api.schedule.SchedulerCreationException"));
        }

    }
}
