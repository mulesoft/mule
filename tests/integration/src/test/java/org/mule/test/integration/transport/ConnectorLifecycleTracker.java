/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
