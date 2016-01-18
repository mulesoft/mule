/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

public class PollingTestCase extends FunctionalTestCase
{
    private static List<String> foo;
    private static List<String> bar;
    private static List<MuleEvent> events;
    private static List<String> eventIds;

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();
        foo = new ArrayList<String>();
        bar = new ArrayList<String>();
        events = new ArrayList<MuleEvent>();
        eventIds = new ArrayList<String>();
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/polling-config.xml";
    }

    @Test
    public void testPolling() throws Exception
    {
        Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(Schedulers.allPollSchedulers());
        assertEquals(4, schedulers.size());

        Thread.sleep(5000);
        synchronized (foo)
        {
            assertTrue(foo.size() > 0);
            for (String s: foo)
            {
                assertEquals("foo", s);
            }
        }
        synchronized (bar)
        {
            assertTrue(bar.size() > 0);
            for (String s: bar)
            {
                assertEquals("bar", s);
            }
        }

        synchronized (events)
        {
            assertTrue(events.size() > 0);
            assertEquals(events.size(), eventIds.size());

            for (int i = 0; i < events.size(); i++)
            {
                assertNotNull(events.get(i));
                assertEquals(events.get(i).getId(), eventIds.get(i));
            }
        }
    }

    public static class FooComponent
    {
        public boolean process(String s)
        {
            synchronized (foo)
            {

                if (foo.size() < 10)
                {
                    foo.add(s);
                    return true;
                }
            }
            return false;
        }
    }

    public static class BarComponent
    {
        public boolean process(String s)
        {
            synchronized (bar)
            {
                if (bar.size() < 10)
                {
                    bar.add(s);
                    return true;
                }
            }
            return false;
        }
    }

    public static class EventWireTrap implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            synchronized (events)
            {
                events.add(RequestContext.getEvent());
                eventIds.add(event.getId());
            }
            return event;
        }
    }

}
