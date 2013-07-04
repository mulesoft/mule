package org.mule.transport.polling.schedule;


import static org.mule.transport.polling.PollingMessageSource.schedulerNameOf;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.SchedulerFactory;
import org.mule.transport.polling.PollingMessageSource;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Implementation of {@link org.mule.api.schedule.SchedulerFactory} for a {@link org.mule.transport.polling.schedule.FixedFrequencyScheduler}.
 * </p>
 *
 * @since 3.5.0
 */
public class FixedFrequencySchedulerFactory extends SchedulerFactory<PollingMessageSource>
{

    /**
     * <p>The {@link TimeUnit} of the scheduler</p>
     */
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    /**
     * <p>The frequency of the scheduler in timeUnit</p>
     */
    private long frequency;

    /**
     * <p>The time in timeUnit that it has to wait before executing the first task</p>
     */
    private long startDelay = 0l;


    @Override
    protected Scheduler doCreate(String name, final PollingMessageSource job)
    {
        FixedFrequencyScheduler fixedFrequencyScheduler = new FixedFrequencyScheduler(name,
                                                                                      frequency, startDelay, new Runnable()
        {
            @Override
            public void run()
            {
                job.run();
            }
        }, timeUnit);
        return fixedFrequencyScheduler;
    }

    public void setTimeUnit(TimeUnit timeUnit)
    {
        this.timeUnit = timeUnit;
    }

    public void setFrequency(long frequency)
    {
        this.frequency = frequency;
    }

    public void setStartDelay(long startDelay)
    {
        this.startDelay = startDelay;
    }

}
