/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.ServiceType;
import org.mule.endpoint.AbstractEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.OutboundEndpointWrapper;
import org.mule.processor.AbstractRedeliveryPolicy;
import org.mule.transport.service.TransportServiceDescriptor;

/**
 * Spring FactoryBean used to create concrete instances of outbound endpoints
 */
public class OutboundEndpointFactoryBean extends AbstractEndpointFactoryBean
{

    public OutboundEndpointFactoryBean(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);
    }

    public OutboundEndpointFactoryBean()
    {
        super();
    }
    
    public Class getObjectType()
    {
        return OutboundEndpoint.class;
    }

    public Object doGetObject() throws Exception
    {
        // If this is a meta endpoint, then we can wrap it using the meta endpoint builder from the TransportServiceDescriptor
        String scheme = getEndpointBuilder().getEndpoint().getFullScheme();
        TransportServiceDescriptor tsd = (TransportServiceDescriptor) muleContext.getRegistry().lookupServiceDescriptor(ServiceType.TRANSPORT, scheme, null);
        EndpointBuilder endpointBuilder = tsd.createEndpointBuilder(this);

        OutboundEndpoint outboundEndpoint = muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
        if (outboundEndpoint instanceof AbstractEndpoint)
        {
            AbstractEndpoint.class.cast(outboundEndpoint).setAnnotations(getAnnotations());
        }
        return new LifecycleAwareOutboundEndpointMessageProcessor(outboundEndpoint);
    }



    @Override
    public void setRedeliveryPolicy(AbstractRedeliveryPolicy redeliveryPolicy)
    {
        throw new IllegalStateException("A redelivery policy cannot be specified for an outbound endpoint.");
    }

    public static class LifecycleAwareOutboundEndpointMessageProcessor extends OutboundEndpointWrapper implements Initialisable, MuleContextAware, MessagingExceptionHandlerAware
    {

        private OutboundEndpoint outboundEndpoint;
        private MessageProcessor executionMessageProcessor;
        private MuleContext muleContext;
        private MessagingExceptionHandler messagingExceptionHandler;

        public LifecycleAwareOutboundEndpointMessageProcessor(OutboundEndpoint outboundEndpoint)
        {
            this.outboundEndpoint = outboundEndpoint;
        }

        @Override
        protected MessageProcessor getExecutionMessageProcessor(MuleEvent event)
        {
            return executionMessageProcessor;
        }

        @Override
        protected OutboundEndpoint getOutboundEndpoint()
        {
            return outboundEndpoint;
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return executionMessageProcessor.process(event);
        }

        @Override
        public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
        {
            this.messagingExceptionHandler = messagingExceptionHandler;
        }

        @Override
        public void initialise() throws InitialisationException
        {
            try
            {
                executionMessageProcessor = muleContext.getOutboundEndpointExecutorFactory().getOutboundEndpointExecutor(outboundEndpoint, messagingExceptionHandler);
            }
            catch (MuleException e)
            {
                throw new InitialisationException(e, this);
            }
        }

        @Override
        public void setMuleContext(MuleContext context)
        {
            this.muleContext = context;
        }

    }

}
