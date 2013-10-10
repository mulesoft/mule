/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.redelivery;

import org.mule.config.i18n.CoreMessages;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.JmsConstants;

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
 * @see org.mule.transport.jms.redelivery.JmsXRedeliveryHandlerFactory
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
