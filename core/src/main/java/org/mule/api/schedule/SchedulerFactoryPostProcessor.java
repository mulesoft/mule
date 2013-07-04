package org.mule.api.schedule;

/**
 * <p>
 * All the classes that implement this interface and are registered in the mule registry are going to be called
 * after the creation of a {@link Scheduler} by the {@link SchedulerFactory}
 * </p>
 * <p>
 * The SchedulerFactoryPostProcessor can alter the instance of the scheduler, create notifications, etc. It is a way
 * of hooking the creation of each {@link Scheduler}
 * </p>
 * <p>
 * The SchedulerFactoryPostProcessor can be:
 * <p/>
 * <p>
 * <ol>
 * <li>Registered by Spring bean or mule module extension via mule configuration (xml)</li>
 * <li>Registered as a bootstrap extension</li>
 * <li>Registered manually in the MuleRegistry.</li>
 * </ol>
 * </p>
 *
 * @since 3.5.0
 */
public interface SchedulerFactoryPostProcessor
{

    /**
     * <p>
     * Process based on a {@link Scheduler} instance after the {@link SchedulerFactory} creates it.
     * </p>
     *
     * @param scheduler The {@link Scheduler} that was created by the {@SchedulerFactory}. It will never be Null.
     * @return A Post processed instance of a {@link Scheduler}
     */
    Scheduler process(Scheduler scheduler);
}
