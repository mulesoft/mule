package org.mule.modules.schedulers.cron;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.SchedulerFactoryPostProcessor;
import org.mule.api.schedule.cluster.ClusterizableScheduler;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.PollingReceiverWorker;

public class PollSchedulerFactoryPostProcessor implements SchedulerFactoryPostProcessor
{

    @Override
    public Scheduler process(final Scheduler scheduler)
    {
        Object job = scheduler.getJob();
        if ( job instanceof PollingReceiverWorker)
        {
            return new Scheduler<PollingReceiverWorker>(){

                @Override
                public void schedule() throws Exception
                {
                    scheduler.schedule();
                }

                @Override
                public PollingReceiverWorker getJob()
                {
                    return (PollingReceiverWorker) scheduler.getJob();
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
        return scheduler;
    }
}

