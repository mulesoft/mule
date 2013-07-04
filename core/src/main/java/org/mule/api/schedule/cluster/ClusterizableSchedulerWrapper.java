package org.mule.api.schedule.cluster;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.schedule.Scheduler;
import org.mule.lifecycle.PrimaryNodeLifecycleNotificationListener;

/**
 * <p>
 * Wrapper for {@link Scheduler} in a custer. All the {@link Scheduler} that implement {@link ClusterizableScheduler}
 * will be automatically wrapped by the {@link org.mule.api.schedule.SchedulerFactory}
 * </p>
 *
 * @since 3.5.0
 */
public class ClusterizableSchedulerWrapper implements Scheduler
{

    /**
     * <p>The {@link MuleContext} needed to check for the primary poll instance</p>
     */
    private MuleContext muleContext;

    /**
     * <p>The {@link ClusterizableScheduler} that is wrapped</p>
     */
    private final ClusterizableScheduler delegateScheduler;

    /**
     * <p>The Primary node listener</p>
     */
    private PrimaryNodeLifecycleNotificationListener listener;

    public ClusterizableSchedulerWrapper(MuleContext muleContext, ClusterizableScheduler delegateScheduler)
    {
        this.muleContext = muleContext;
        this.delegateScheduler = delegateScheduler;
    }

    @Override
    public void schedule() throws Exception
    {
        delegateScheduler.schedule();
    }

    @Override
    public void dispose()
    {
        delegateScheduler.dispose();
        listener.unregister();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        delegateScheduler.initialise();

        listener = new PrimaryNodeLifecycleNotificationListener(new Startable()
        {
            @Override
            public void start() throws MuleException
            {
                if (muleContext.isPrimaryPollingInstance())
                {
                    delegateScheduler.start();
                }
            }
        }, muleContext);
        listener.register();
    }

    @Override
    public void setName(String name)
    {
        delegateScheduler.setName(name);
    }

    @Override
    public String getName()
    {
        return delegateScheduler.getName();
    }

    @Override
    public void start() throws MuleException
    {
        delegateScheduler.start();
    }

    @Override
    public void stop() throws MuleException
    {
        delegateScheduler.stop();
    }
}
