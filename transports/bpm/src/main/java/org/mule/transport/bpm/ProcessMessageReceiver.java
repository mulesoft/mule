/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm;

import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.ConnectorException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.MessageFactory;
import org.mule.session.DefaultMuleSession;
import org.mule.transport.AbstractMessageReceiver;

import java.util.Map;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

/** Generates an incoming Mule event from an executing workflow process. */
public class ProcessMessageReceiver extends AbstractMessageReceiver
{

    private ProcessConnector connector = null;

    public ProcessMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        this.connector = (ProcessConnector) connector;
    }

    public MuleMessage generateSynchronousEvent(String endpoint, Object payload, Map messageProperties) throws MuleException
    {
        logger.debug("Executing process is sending an event (synchronously) to Mule endpoint = " + endpoint);
        MuleMessage response = generateEvent(endpoint, payload, messageProperties, 
            MessageExchangePattern.REQUEST_RESPONSE);
        if (logger.isDebugEnabled())
        {
            logger.debug("Synchronous response is " + (response != null ? response.getPayload() : null));
        }
        return response;
    }

    public void generateAsynchronousEvent(String endpoint, Object payload, Map messageProperties) throws MuleException, WorkException
    {
        logger.debug("Executing process is dispatching an event (asynchronously) to Mule endpoint = " + endpoint);
        WorkManager workManager = getWorkManager();
        if (workManager != null)
        {
            workManager.scheduleWork(new Worker(endpoint, payload, messageProperties));
        }
        else
        {
            throw new ConnectorException(MessageFactory.createStaticMessage("WorkManager not available"), getConnector());
        }
    }

    protected MuleMessage generateEvent(String endpoint, Object payload, Map messageProperties, MessageExchangePattern exchangePattern) throws MuleException
    {
        MuleMessage message;
        if (payload instanceof MuleMessage)
        {
            message = (MuleMessage) payload;
        }
        else
        {
            message = createMuleMessage(payload, this.endpoint.getEncoding());
        }
        message.addProperties(messageProperties, PropertyScope.INBOUND);
        message.addProperties(messageProperties, PropertyScope.INVOCATION);

        //TODO should probably cache this
        EndpointBuilder endpointBuilder = connector.getMuleContext().getRegistry().lookupEndpointFactory().getEndpointBuilder(endpoint);
        endpointBuilder.setExchangePattern(exchangePattern);
        OutboundEndpoint ep = endpointBuilder.buildOutboundEndpoint();
       
        DefaultMuleEvent event = new DefaultMuleEvent(message, ep, new DefaultMuleSession(flowConstruct, connector.getMuleContext()));

        // Set correlation properties in SESSION scope so that they get propagated to response messages.
        RequestContext.setEvent(event);
        if (messageProperties.get(ProcessConnector.PROPERTY_PROCESS_TYPE) != null)
        {
            event.getMessage().setSessionProperty(ProcessConnector.PROPERTY_PROCESS_TYPE, messageProperties.get(ProcessConnector.PROPERTY_PROCESS_TYPE));
        }
        if (messageProperties.get(ProcessConnector.PROPERTY_PROCESS_ID) != null)
        {
            event.getMessage().setSessionProperty(ProcessConnector.PROPERTY_PROCESS_ID, messageProperties.get(ProcessConnector.PROPERTY_PROCESS_ID));
        }
        
        MuleEvent resultEvent = ep.process(event);
        
        MuleMessage response = null;
        if (resultEvent != null)
        {
            response = resultEvent.getMessage();
            if (response.getExceptionPayload() != null)
            {
                throw new ConnectorException(MessageFactory.createStaticMessage("Unable to send or route message"), getConnector(), response.getExceptionPayload().getRootException());
            }
        }
        
        return response;
    }

    private class Worker implements Work
    {
        private String endpoint;
        private Object payload;
        private Map messageProperties;

        public Worker(String endpoint, Object payload, Map messageProperties)
        {
            this.endpoint = endpoint;
            this.payload = payload;
            this.messageProperties = messageProperties;
        }

        public void run()
        {
            try
            {
                generateEvent(endpoint, payload, messageProperties, MessageExchangePattern.ONE_WAY);
            }
            catch (Exception e)
            {
                getConnector().getMuleContext().getExceptionListener().handleException(e);
            }
        }

        public void release()
        { /*nop*/ }
    }
}
