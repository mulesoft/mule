package org.mule.api.schedule;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.schedule.cluster.ClusterizableFactoryPostProcessor;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * Factory of schedulers. Every Scheduler should be created by a SchedulerFactory if the creation process allows
 * post creation hooking.
 * </p>
 * <p>
 * Once the Factory creates a scheduler it calls a set of {@link SchedulerFactoryPostProcessor} that might affect the
 * instance of the scheduler.
 * </p>
 * <p>
 * Apart from the set of {@link SchedulerFactoryPostProcessor} in the registry the factory processes internal
 * {@link SchedulerFactoryPostProcessor}. For example, {@link ClusterizableFactoryPostProcessor}
 * </p>
 * <p>
 * The {@link SchedulerFactory} also registers the {@link Scheduler} into the mule registry.
 * </p>
 *
 * @param <T> is the type of the scheduler job (what is the scheduler going to execute)
 * @since 3.5.0
 */
public abstract class SchedulerFactory<T> implements MuleContextAware
{

    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * <p>
     * Mule context. Should never be null. In case of being null then the post processing is discarded
     * </p>
     */
    private MuleContext context;

    /**
     * <p>
     * Creates a scheduler for a job and runs all the registered post processors.
     * </p>
     *
     * @param job  The {@link Scheduler} job that has to be executed.
     * @param name The {@link Scheduler} name. This name is the one that is going to be use to register the {@link Scheduler}
     *             in the {@link org.mule.api.registry.MuleRegistry}
     * @return A new instance of a {@link Scheduler}. It must never be null.
     * @throws SchedulerCreationException In case after creating and post processing the {@link Scheduler} it is null or
     *                                    in case a {@link SchedulerFactoryPostProcessor} fails.
     */
    public final Scheduler create(String name, T job) throws SchedulerCreationException
    {
        Scheduler scheduler = doCreate(name, job);

        checkNull(scheduler);


        Scheduler postProcessedScheduler = postProcess(scheduler);

        checkNull(postProcessedScheduler);

        return postProcessedScheduler;
    }

    /**
     * <p>
     * Template method to delegate the creation of the {@link Scheduler}. This method is thought to create an instance
     * of a {@link Scheduler}. It should not Start/Stop it.
     * </p>
     *
     * @param name
     * @param job  The Job the {@link org.mule.api.schedule.Scheduler} is going to execute
     * @return The {@link Scheduler} instance
     */
    protected abstract Scheduler doCreate(String name, T job);

    private Scheduler postProcess(Scheduler scheduler)
    {
        if (context == null)
        {
            return scheduler;
        }

        scheduler = runInnerPostProcessors(scheduler);

        Map<String, SchedulerFactoryPostProcessor> postProcessors = context.getRegistry()
                .lookupByType(SchedulerFactoryPostProcessor.class);
        for (SchedulerFactoryPostProcessor postProcessor : postProcessors.values())
        {
            scheduler = postProcessor.process(scheduler);
            checkNull(scheduler);
        }

        registerScheduler(scheduler);

        return scheduler;
    }

    private void registerScheduler(Scheduler scheduler)
    {
        try
        {
            context.getRegistry().registerScheduler(scheduler);
        }
        catch (MuleException e)
        {
            logger.error("Could not register the scheduler " + scheduler.getName() + " in the registry", e);
        }
    }

    private Scheduler runInnerPostProcessors(Scheduler scheduler)
    {
        scheduler = new ClusterizableFactoryPostProcessor(context).process(scheduler);
        return scheduler;
    }

    private void checkNull(Scheduler postProcessedScheduler)
    {
        if (postProcessedScheduler == null)
        {
            throw new SchedulerCreationException("The scheduler cannot be null");
        }
    }


    @Override
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }


}
