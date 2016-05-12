/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.transport.AbstractConnector;

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

    @Override
    public String getProtocol()
    {
        return "test";
    }

    @Override
    public void doConnect() throws Exception
    {
        connected = true;
        getTracker().add("connect");
    }

    @Override
    public void doDisconnect() throws Exception
    {
        connected = false;
        getTracker().add("disconnect");
    }



    public void setProperty(final String value) {
        tracker.add("setProperty");
    }

    @Override
    public void doInitialise() throws InitialisationException
    {
        tracker.add("initialise");
    }

    @Override
    public void doStart() throws MuleException
    {
        tracker.add("start");
    }

    @Override
    public void doStop() throws MuleException {
        tracker.add("stop");
    }

    @Override
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
