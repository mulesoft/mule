/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration.activemq;

import org.mule.transport.jms.integration.JmsVendorConfiguration;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;

/**
 * Abstracts all the Jms Vendor specific configuration.  This is the implementation for ActiveMQ.
 */
public class ActiveMQJmsConfiguration implements JmsVendorConfiguration
{
    public static final String DEFAULT_BROKER_URL = "vm://localhost?broker.persistent=false&broker.useJmx=false";

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

    /**
     * Timeout used when checking that a message is NOT present
     *
     * @return
     */
    public long getSmallTimeout()
    {
        return 1000L;
    }

    /**
     * The timeout used when waiting for a message to arrive
     *
     * @return
     */
    public long getTimeout()
    {
        return 5000L;
    }

    public String getProtocol()
    {
        return "jms";
    }

    public String getProviderName()
    {
        return "activemq";
    }

    public Map getProperties()
    {
        return new HashMap();
    }
}