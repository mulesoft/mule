/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.cron;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;


/**
 * <p>
 * Uses the API to stop and start cron schedulers.
 * </p>
 */
public class CronsSchedulerTest extends FunctionalTestCase
{
    private static List<String> foo = new ArrayList<String>();
    private static List<String> bar = new ArrayList<String>();

    @BeforeClass
    public static void setProperties()
    {
        System.setProperty("expression.property", "0/1 * * * * ?");
    }

    @Override
    protected String getConfigFile()
    {
        return "cron-scheduler-config.xml";
    }

    @Test
    public void test() throws Exception
    {
        waitForPollElements();

        checkForFooCollectionToBeFilled();
        checkForBarCollectionToBeFilled();

        stopSchedulers();

        waitForPollElements();

        int fooElementsAfterStopping = foo.size();

        waitForPollElements();

        assertEquals(fooElementsAfterStopping, foo.size());

        runSchedulersOnce();

        waitForPollElements();

        assertEquals(fooElementsAfterStopping + 1, foo.size());
    }

    private void waitForPollElements() throws InterruptedException
    {
        Thread.sleep(2000);
    }

    private void checkForFooCollectionToBeFilled()
    {
        synchronized (foo)
        {
            foo.size();
            assertTrue(foo.size() > 0);
            for (String s : foo)
            {
                assertEquals("foo", s);
            }
        }
    }

    private void checkForBarCollectionToBeFilled()
    {
        synchronized (bar)
        {
            bar.size();
            assertTrue(bar.size() > 0);
            for (String s : bar)
            {
                assertEquals("bar", s);
            }
        }
    }

    private void runSchedulersOnce() throws Exception
    {
        Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(
                Schedulers.flowConstructPollingSchedulers("pollfoo"));

        for (Scheduler scheduler : schedulers)
        {
            scheduler.schedule();
        }
    }

    private void stopSchedulers() throws MuleException
    {
        Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(
                Schedulers.flowConstructPollingSchedulers("pollfoo"));

        for (Scheduler scheduler : schedulers)
        {
            scheduler.stop();
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
}
