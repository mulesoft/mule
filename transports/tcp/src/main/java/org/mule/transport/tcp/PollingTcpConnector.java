/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;

import java.util.Properties;

/**
 * <code>PollingTcpMessageReceiver</code> acts as a polling TCP connector.
 * 
 * @author esteban.robles
 */
public class PollingTcpConnector extends TcpConnector
{
    /**
     * How long to wait in milliseconds between make a new request
     */
    private long pollingFrequency = 1000L;

    public PollingTcpConnector(MuleContext context)
    {
        super(context);
        serviceOverrides = new Properties();
        serviceOverrides.setProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS,
            PollingTcpMessageReceiver.class.getName());
    }

    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }
}
