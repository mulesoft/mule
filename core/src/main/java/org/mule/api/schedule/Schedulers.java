/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
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
     *         for a particular {@link org.mule.api.construct.FlowConstruct}
     */
    public static Predicate<String> flowConstructPollingSchedulers(final String flowConstruct)
    {
        return new Predicate<String>()
        {
            @Override
            public boolean evaluate(String s)
            {
                return s.startsWith(MessageProcessorPollingMessageReceiver.POLLING_TRANSPORT + "://" + flowConstruct + "/");
            }
        };
    }
}
