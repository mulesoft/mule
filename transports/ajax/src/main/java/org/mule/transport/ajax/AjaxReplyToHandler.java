/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ajax;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.DefaultReplyToHandler;

import java.util.List;

import org.cometd.Channel;
import org.cometd.Client;
import org.mortbay.cometd.AbstractBayeux;

/**
 * Handles the sending of sending result messages back to the client when the a replyTo 
 * channel is specified in the client request.
 */
public class AjaxReplyToHandler extends DefaultReplyToHandler
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;
    
    public AjaxReplyToHandler(Connector connector)
    {
        super(connector.getMuleContext());
        this.connector = connector;
    }

    @Override
    public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
    {
        AbstractBayeux bayeux = ((BayeuxAware)connector).getBayeux();
        Channel channel = bayeux.getChannel(replyTo.toString(), false);
        if(channel==null)
        {
            logger.warn("No ajax Channel: " + replyTo + ". Maybe the client unregistered interest.");
            return;
        }
        
        Object ret;
        if(returnMessage.getExceptionPayload()!=null)
        {
            //If we are using RPC make sure we still send something back to the client so that the subscription is cancelled
            ret = returnMessage.getExceptionPayload().getMessage();
        }
        else
        {   
            EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(event.getMessageSourceURI().toString(), muleContext);
            endpointBuilder.setConnector(connector);
            OutboundEndpoint tempEndpoint = muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
            
            List<Transformer> defaultTransportTransformers =  ((org.mule.transport.AbstractConnector) connector).getDefaultOutboundTransformers(tempEndpoint);
            
            returnMessage.applyTransformers(event, defaultTransportTransformers);

            ret = returnMessage.getPayload();
        }
        //Publish to interested clients
        for (Client client : channel.getSubscribers())
        {
            channel.publish(client, ret, null);
        }
    }
}
