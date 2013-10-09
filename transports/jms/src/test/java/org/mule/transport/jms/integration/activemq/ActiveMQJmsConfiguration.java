/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration.activemq;

import org.mule.transport.jms.integration.JmsVendorConfiguration;

import java.util.Collections;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;

/**
 * Abstracts all the Jms Vendor specific configuration.  This is the implementation for ActiveMQ.
 */
public class ActiveMQJmsConfiguration implements JmsVendorConfiguration
{
    public static final String DEFAULT_BROKER_URL = "vm://localhost?broker.persistent=false&broker.useJmx=false";

    public void initialise(Class callingClass) throws Exception
    {
        // empty
    }
    
    public Connection getConnection(boolean topic, boolean xa) throws Exception
    {
        if (xa)
        {
            return new ActiveMQXAConnectionFactory(DEFAULT_BROKER_URL).createConnection();

        }
        else
        {
            return new ActiveMQConnectionFactory(DEFAULT_BROKER_URL).createConnection();
        }
    }

    public String getInboundEndpoint()
    {
        return getProtocol() + "://" + getInboundDestinationName();
    }

    public String getOutboundEndpoint()
    {
        return getProtocol() + "://" + getOutboundDestinationName();
    }

    public String getMiddleEndpoint()
    {
        return getProtocol() + "://" + getMiddleDestinationName();
    }

    public String getTopicBroadcastEndpoint()
    {
        return getProtocol() + "://topic:" + getBroadcastDestinationName();
    }

    public String getDeadLetterEndpoint()
    {
        return getProtocol() + "://" + getDeadLetterDestinationName();
    }

    public String getInboundDestinationName()
    {
        return "in";
    }

    public String getOutboundDestinationName()
    {
        return "out";
    }

    public String getMiddleDestinationName()
    {
        return "middle";
    }

    public String getBroadcastDestinationName()
    {
        return "broadcast";
    }

    public String getDeadLetterDestinationName()
    {
        return "dlq";
    }

    /**
     * Timeout used when checking that a message is NOT present
     */
    public long getSmallTimeout()
    {
        return 1000L;
    }

    /**
     * The timeout used when waiting for a message to arrive
     */
    public long getTimeout()
    {
        return 5000L;
    }

    public String getProtocol()
    {
        return "jms";
    }

    public String getName()
    {
        return "activemq";
    }

    public Map getProperties()
    {
        return Collections.EMPTY_MAP;
    }

    public ConnectionFactory getTestConnectionFactory()
    {
        return new ActiveMQTestReconnectionConnectionFactoryWrapper();       
    }
    
    public boolean isEnabled()
    {
        return true;
    }
}
