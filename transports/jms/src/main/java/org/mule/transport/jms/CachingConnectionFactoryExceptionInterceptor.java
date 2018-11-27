/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.DelegatingConnectionFactory;

public class CachingConnectionFactoryExceptionInterceptor extends DelegatingConnectionFactory implements ExceptionListener
{

    private AggregatedExceptionListener aggregatedExceptionListener;

    CachingConnectionFactoryExceptionInterceptor(CachingConnectionFactory target)
    {
        // Register connection factory exception listener delegate.
        aggregatedExceptionListener = new AggregatedExceptionListener(target);
    }

    public void registerConnector(JmsConnector aConnector)
    {
        if (!aggregatedExceptionListener.isRegistered(aConnector))
        {
            aggregatedExceptionListener.registerConnector(aConnector);
        }
    }

    @Override
    public void onException(JMSException e)
    {
        aggregatedExceptionListener.onException(e);
    }

    private class AggregatedExceptionListener implements ExceptionListener
    {
        private Set<JmsConnector> connectorDelegates = new LinkedHashSet<>(2);

        private ExceptionListener factoryDelegate;

        AggregatedExceptionListener(ExceptionListener aConnectionFactory)
        {
            factoryDelegate = aConnectionFactory;
        }

        public void registerConnector(JmsConnector aDelegate)
        {
            connectorDelegates.add(aDelegate);
        }

        public boolean isRegistered(ExceptionListener aListener)
        {
            return connectorDelegates.contains(aListener);
        }

        @Override
        public void onException(JMSException e)
        {
            // Prevent connectors from dispatching message in case an exception is being handled
            for (JmsConnector aConnector : new LinkedHashSet<JmsConnector>(connectorDelegates))
            {
                aConnector.setExceptionIsBeingHandled(true);
            }

            // Delegate exception to CachingConnectionFactory
            factoryDelegate.onException(e);

            // Delegate exception handling to connectors. Similar to SingleConnectionFactory$AggregatedExceptionListener,
            // in which a copy of the delegates list is iterated to avoid ConcurrentModificationException.
            for (ExceptionListener aConnectorListener : new LinkedHashSet<ExceptionListener>(connectorDelegates))
            {
                aConnectorListener.onException(e);
            }
        }
    }
}
