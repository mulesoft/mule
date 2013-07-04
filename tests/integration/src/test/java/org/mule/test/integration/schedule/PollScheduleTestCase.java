package org.mule.test.integration.schedule;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleException;
import org.mule.api.schedule.Scheduler;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.polling.PollingMessageSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

public class PollScheduleTestCase extends FunctionalTestCase
{

    private static List<String> foo = new ArrayList<String>();

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/schedule/polling-schedule-config.xml";
    }


    @Test
    public void test() throws Exception
    {
        Thread.sleep(2000);

        synchronized (foo)
        {
            foo.size();
            assertTrue(foo.size() > 0);
            for (String s : foo)
            {
                assertEquals(s, "foo");
            }
        }

        stopSchedulers();

        Thread.sleep(2000);

        int count = foo.size();

        Thread.sleep(2000);

        assertEquals(count, foo.size());
        runSchedulers();

        Thread.sleep(2000);

        assertEquals(count + 1, foo.size());


    }

    private void runSchedulers() throws Exception
    {
        Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(
                PollingMessageSource.flowPollingSchedulers("pollfoo"));

        for (Scheduler scheduler : schedulers)
        {
            scheduler.schedule();
        }
    }

    private void stopSchedulers() throws MuleException
    {
        Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(
                PollingMessageSource.flowPollingSchedulers("pollfoo"));

        for (Scheduler scheduler : schedulers)
        {
            scheduler.stop();
        }
    }

    public static class FooComponent
    {

        public boolean process(String s)
        {
            System.out.println(System.currentTimeMillis());

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
}
