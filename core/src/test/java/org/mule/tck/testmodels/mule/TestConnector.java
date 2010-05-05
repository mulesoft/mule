/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageDispatcherFactory;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.service.TransportServiceDescriptor;

/**
 * <code>TestConnector</code> use a mock connector
 */
public class TestConnector extends AbstractConnector
{
    public static final String TEST = "test";

    private String someProperty;

    private int initialiseCount = 0;
    private int connectCount = 0;
    private int startCount = 0;
    private int stopCount = 0;
    private int disconnectCount = 0;
    private int disposeCount = 0;
    
    public TestConnector(MuleContext context)
    {
        super(context);
        setDispatcherFactory(new AbstractMessageDispatcherFactory()
        {
            @Override
            public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
            {
                return new TestMessageDispatcher(endpoint);
            }
        });
    }

    public String getProtocol()
    {
        return TEST;
    }

    @Override
    protected void doInitialise() 
    {
        initialiseCount++;
    }

    @Override
    protected void doConnect() 
    {
        connectCount++;
    }

    @Override
    protected void doStart() 
    {
        startCount++;
    }

    @Override
    protected void doStop() 
    {
        stopCount++;
    }

    @Override
    protected void doDisconnect() 
    {
        disconnectCount++;
    }

    @Override
    protected void doDispose() 
    {
        disposeCount++;
    }

    public String getSomeProperty()
    {
        return someProperty;
    }

    public void setSomeProperty(String someProperty)
    {
        this.someProperty = someProperty;
    }

    @Override
    public MessageReceiver createReceiver(Service service, InboundEndpoint endpoint) throws Exception
    {
        MessageReceiver receiver = new AbstractMessageReceiver(this, service, endpoint)
        {

            @Override
            protected void doInitialise() throws InitialisationException
            {
                //nothing to do
            }

            @Override
            protected void doConnect() throws Exception
            {
                // nothing to do
            }

            @Override
            protected void doDisconnect() throws Exception
            {
                // nothing to do
            }

            @Override
            protected void doStart() throws MuleException
            {
                // nothing to do
            }

            @Override
            protected void doStop() throws MuleException
            {
                // nothing to do
            }

            @Override
            protected void doDispose()
            {
                // nothing to do               
            }
        };
        return receiver;
    }

    /**
     * Open up the access to the service descriptor for testing purposes.
     */
    @Override
    public TransportServiceDescriptor getServiceDescriptor()
    {
        return super.getServiceDescriptor();
    }

    public void destroyReceiver(MessageReceiver receiver, InboundEndpoint endpoint) throws Exception
    {
        // nothing to do
    }

    public int getInitialiseCount() 
    {
        return initialiseCount;
    }
    
    public int getConnectCount() 
    {
        return connectCount;
    }
    
    public int getStartCount() 
    {
        return startCount;
    }
    
    public int getStopCount() 
    {
        return stopCount;
    }
    
    public int getDisconnectCount() 
    {
        return disconnectCount;
    }
    
    public int getDisposeCount() 
    {
        return disposeCount;
    }
}
