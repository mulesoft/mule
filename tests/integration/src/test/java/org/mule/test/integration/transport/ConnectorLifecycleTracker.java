/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.transport.AbstractConnector;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
public class ConnectorLifecycleTracker extends AbstractConnector
{
    private final List<String> tracker = new ArrayList<String>();

    private String property1;

    boolean connected = false;
    
    public ConnectorLifecycleTracker(MuleContext context)
    {
        super(context);
    }
    
    public List<String> getTracker() {
        return tracker;
    }

    public String getProtocol()
    {
        return "test";
    }

    public void doConnect() throws Exception
    {
        connected = true;
        getTracker().add("connect");
    }

    public void doDisconnect() throws Exception
    {
        connected = false;
        getTracker().add("disconnect");
    }



    public void setProperty(final String value) {
        tracker.add("setProperty");
    }

    public void doInitialise() throws InitialisationException
    {
        tracker.add("initialise");
    }

    public void doStart() throws MuleException
    {
        tracker.add("start");
    }

    public void doStop() throws MuleException {
        tracker.add("stop");
    }

    public void doDispose() {
        tracker.add("dispose");
    }


    public String getProperty1()
    {
        return property1;
    }

    public void setProperty1(String property1)
    {
        tracker.add("setProperty");
        this.property1 = property1;
    }

    public MessageProcessor getOutboundEndpointMessageProcessor(OutboundEndpoint endpoint)
        throws MuleException
    {
        return null;
    }
}
