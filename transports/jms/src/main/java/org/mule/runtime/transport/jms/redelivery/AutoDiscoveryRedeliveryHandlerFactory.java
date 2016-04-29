/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.redelivery;

import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.transport.jms.JmsConnector;
import org.mule.runtime.transport.jms.JmsConstants;

import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This factory will consult JMS connection metadata for supported optional properties and use
 * those, if available, otherwise falling back to the manual counting of redeliveries.
 *
 * @see CountingRedeliveryHandlerFactory
 * @see org.mule.runtime.transport.jms.redelivery.JmsXRedeliveryHandlerFactory
 * @see javax.jms.ConnectionMetaData
 */
public class AutoDiscoveryRedeliveryHandlerFactory implements RedeliveryHandlerFactory
{
    protected final Log logger = LogFactory.getLog(getClass());

    protected AtomicReference<RedeliveryHandler> delegateHandler = new AtomicReference<RedeliveryHandler>(null);

    protected JmsConnector connector;

    public AutoDiscoveryRedeliveryHandlerFactory(JmsConnector connector)
    {
        if (connector == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("connector").getMessage());
        }
        this.connector = connector;
    }

    @Override
    public RedeliveryHandler create()
    {
        RedeliveryHandler result;

        // initialize, accounting for concurrency
        if (delegateHandler.get() == null)
        {
            RedeliveryHandler newInstance = createInstance();
            boolean ok = delegateHandler.compareAndSet(null, newInstance);
            if (!ok)
            {
                // someone was faster to initialize it, use this ref instead
                result = delegateHandler.get();
            }
            else
            {
                result = newInstance;
            }
        }
        else
        {
            // just re-use existing handler
            result = delegateHandler.get();
        }

        return result;
    }

    /**
     * Create an instance using the discovery mechanism.
     *
     * @return an implementation based on the results of discovery
     */
    protected RedeliveryHandler createInstance()
    {
        RedeliveryHandler newInstance;
        try
        {
            ConnectionMetaData metaData = connector.getConnection().getMetaData();
            boolean supportsDeliveryCount = false;
            final Enumeration propNames = metaData.getJMSXPropertyNames();
            while (propNames.hasMoreElements())
            {
                String p = (String) propNames.nextElement();
                if (JmsConstants.JMS_X_DELIVERY_COUNT.equals(p))
                {
                    supportsDeliveryCount = true;
                    break;
                }
            }

            newInstance = (supportsDeliveryCount) ? new JmsXRedeliveryHandler() : new CountingRedeliveryHandler();
        }
        catch (JMSException e)
        {
            // fallback to defaults
            newInstance = new CountingRedeliveryHandler();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Using " + newInstance.getClass().getName());
        }

        return newInstance;
    }

}
