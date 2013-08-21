package org.mule.modules.schedulers.cron;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.SchedulerFactoryPostProcessor;

public class PollSchedulerFactoryPostProcessor implements SchedulerFactoryPostProcessor
{

    @Override
    public Scheduler process(Object job,final Scheduler scheduler)
    {
        return new Scheduler()
        {

            @Override
            public void schedule() throws Exception
            {
                scheduler.schedule();
            }

            @Override
            public void dispose()
            {
                scheduler.dispose();
            }

            @Override
            public void initialise() throws InitialisationException
            {
                scheduler.initialise();
            }

            @Override
            public void setName(String s)
            {
                scheduler.setName(s);
            }

            @Override
            public String getName()
            {
                return scheduler.getName();
            }

            @Override
            public void start() throws MuleException
            {
                // Does Nothing
            }

            @Override
            public void stop() throws MuleException
            {
                // Does Nothing
            }
        };

    }
}

