/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
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
    private Connector connector;
    
    public AjaxReplyToHandler(List<Transformer> transformers, Connector connector)
    {
        super(transformers, connector.getMuleContext());
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
            returnMessage.applyTransformers(event, getTransformers());
            ret = returnMessage.getPayload();
        }
        //Publish to interested clients
        for (Client client : channel.getSubscribers())
        {
            channel.publish(client, ret, null);
        }
    }
}
