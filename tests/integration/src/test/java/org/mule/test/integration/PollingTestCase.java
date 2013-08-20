/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.polling.MessageProcessorPollingMessageReceiver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

public class PollingTestCase extends FunctionalTestCase
{

    private static List<String> foo = new ArrayList<String>();
    private static List<String> bar = new ArrayList<String>();

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/polling-config.xml";
    }

    @Test
    public void testPolling() throws Exception
    {
        Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(Schedulers.allPollSchedulers());
        assertEquals(3, schedulers.size());

        Thread.sleep(5000);
        synchronized (foo)
        {
            assertTrue(foo.size() > 0);
            for (String s: foo)
            {
                assertEquals(s, "foo");
            }
        }
        synchronized (bar)
        {
            assertTrue(bar.size() > 0);
            for (String s: bar)
            {
                assertEquals(s, "bar");
            }
        }
    }

    public static class FooComponent
    {
        public boolean process(String s)
        {
            try
            {
                Thread.sleep(6000);
            }
            catch (InterruptedException e)
            {

            }
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
            System.out.print(System.currentTimeMillis());

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
}
