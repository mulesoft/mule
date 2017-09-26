/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.cron;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.api.MuleContext;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.tck.junit4.ApplicationContextBuilder;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import java.util.HashMap;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.o;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CronsTimeZoneSchedulerTest
{
    final static String TIMEZONE_XML="America/Argentina/Buenos_Aires";


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void validTimeZoneInScheduler() throws Exception
    {
        ApplicationContextBuilder builder = new ApplicationContextBuilder();
        builder.setApplicationResources(new String[] {"CronSchedulerValidTimeZone"});
        builder.build();
        MuleContext test = builder.build();
        assertThat(test.getRegistry().lookupByType(CronSchedulerFactory.class).size(), equalTo(1));
        assertThat(test.getRegistry().lookupByType(CronScheduler.class).values().iterator().next().getTimeZone().getID(), equalTo(TIMEZONE_XML));
    }

    @Test
    public void invalidTimeZoneInScheduler() throws Exception
    {
        ApplicationContextBuilder builder = new ApplicationContextBuilder();
        builder.setApplicationResources(new String[] {"cron-timezone-scheduler-config.xml"});
        thrown.expect(LifecycleException.class);

    }
}
