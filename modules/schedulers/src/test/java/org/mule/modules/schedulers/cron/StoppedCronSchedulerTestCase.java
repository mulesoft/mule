package org.mule.modules.schedulers.cron;

import static junit.framework.Assert.assertEquals;

import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;


public class StoppedCronSchedulerTestCase  extends FunctionalTestCase
{
    private static List<String> foo = new ArrayList<String>();

    @Override
    protected String getConfigResources()
    {
        return "cron-scheduler-stopped-config.xml";
    }

    @Test
    public void test() throws Exception
    {
        runSchedulersOnce();
        Thread.sleep(6000);

        assertEquals(1, foo.size());
    }


    public static class FooComponent
    {

        public boolean process(String s)
        {
            synchronized (foo)
            {

                foo.add(s);

            }

            return false;
        }
    }

    private void runSchedulersOnce() throws Exception
    {
        Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(
                Schedulers.flowPollingSchedulers("pollfoo"));

        for (Scheduler scheduler : schedulers)
        {
            scheduler.schedule();
        }
    }
}
