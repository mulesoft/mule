package org.mule.api.schedule;

/**
 * <p>
 * This exception is thrown if a {@link Scheduler} could not be created.
 * </p>
 *
 * @since 3.5.0
 */
public class SchedulerCreationException extends RuntimeException
{

    public SchedulerCreationException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

    public SchedulerCreationException(String s)
    {
        super(s);
    }
}
