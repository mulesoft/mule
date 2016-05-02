/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.transport.MessageDispatcher;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.api.transport.MessageRequester;
import org.mule.runtime.core.endpoint.AbstractEndpoint;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.runtime.core.transport.AbstractMessageDispatcherFactory;
import org.mule.runtime.core.transport.AbstractMessageReceiver;
import org.mule.runtime.core.transport.AbstractMessageRequesterFactory;
import org.mule.runtime.core.transport.ConfigurableKeyedObjectPool;
import org.mule.runtime.core.transport.service.TransportServiceDescriptor;

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
    
    private boolean failAtStartup = false;
    
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

        setRequesterFactory(new AbstractMessageRequesterFactory()
        {
            @Override
            public MessageRequester create(InboundEndpoint endpoint) throws MuleException
            {
                return new TestMessageRequester(endpoint);
            }
        });

        setRetryPolicyTemplate((RetryPolicyTemplate) muleContext.getRegistry().lookupObject(
                                MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE));
    }

    @Override
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
        if (isFailAtStartup())
        {
            throw new RuntimeException("Startup failure");
        }
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
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        if (endpoint.getProperty("competingConsumers") != null)
        {
            return flowConstruct.getName() + "~" + endpoint.getEndpointURI().getAddress();
        }
        else
        {
            return super.getReceiverKey(flowConstruct, endpoint);
        }
    }

    @Override
    public MessageReceiver createReceiver(FlowConstruct flowConstuct, InboundEndpoint endpoint) throws Exception
    {
        MessageReceiver receiver = new AbstractMessageReceiver(this, flowConstuct, endpoint)
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

    public ConfigurableKeyedObjectPool getDispatchers()
    {
        return dispatchers;
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

    public MessageProcessor getOutboundEndpointMessageProcessor(OutboundEndpoint endpoint)
        throws MuleException
    {
        return ((AbstractEndpoint) endpoint).getMessageProcessorChain(null);
    }

    public void setFailAtStartup(boolean failAtStartup)
    {
        this.failAtStartup = failAtStartup;
    }

    public boolean isFailAtStartup()
    {
        return failAtStartup;
    }    
}
