package org.mule.api.schedule;


import org.mule.transport.polling.MessageProcessorPollingMessageReceiver;
import org.mule.util.Predicate;

/**
 * <p>
 *     Utility class to create {@link Scheduler} predicates
 * </p>
 */
public class Schedulers
{
    /**
     * @return Predicate used to request the  {@link org.mule.api.registry.MuleRegistry} all the polling {@link org.mule.api.schedule.Scheduler}
     */
    public static Predicate<String> allPollSchedulers()
    {
        return new Predicate<String>()
        {
            @Override
            public boolean evaluate(String s)
            {
                return s.startsWith(MessageProcessorPollingMessageReceiver.POLLING_TRANSPORT + "://");
            }
        };
    }

    /**
     * @return Predicate used to request the  {@link org.mule.api.registry.MuleRegistry} all the polling {@link org.mule.api.schedule.Scheduler}
     *         for a particular {@link org.mule.construct.Flow}
     */
    public static Predicate<String> flowPollingSchedulers(final String flowName)
    {
        return new Predicate<String>()
        {
            @Override
            public boolean evaluate(String s)
            {
                return s.startsWith(MessageProcessorPollingMessageReceiver.POLLING_TRANSPORT + "://" + flowName);
            }
        };
    }
}
