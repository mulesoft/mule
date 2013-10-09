/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
