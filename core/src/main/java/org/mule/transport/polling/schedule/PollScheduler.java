package org.mule.transport.polling.schedule;

import org.mule.api.schedule.Scheduler;
import org.mule.transport.PollingReceiverWorker;


/**
 * <p>
 *     Abstract definition of a Scheduler for poll.
 * </p>
 *
 * @since 3.5.0
 */
public abstract class PollScheduler implements Scheduler<PollingReceiverWorker>
{

    protected PollingReceiverWorker job;

    /**
     * <p>The {@link org.mule.api.schedule.Scheduler} name used as an identifier in the {@link org.mule.api.registry.MuleRegistry}</p>
     */
    protected String name;

    protected PollScheduler(String name,PollingReceiverWorker job)
    {
        this.name = name;
        this.job = job;
    }

    @Override
    public PollingReceiverWorker getJob()
    {
        return job;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
