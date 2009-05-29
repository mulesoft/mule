/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.transport.DefaultReplyToHandler;
import org.mule.transport.ajax.container.AjaxServletConnector;

import dojox.cometd.Channel;
import dojox.cometd.Client;

import java.util.List;

import org.mortbay.cometd.AbstractBayeux;

/**
 * Handles the sending of sending result messages back to the client when the a replyTo 
 * channel is specified in the client request.
 */
public class AjaxReplyToHandler extends DefaultReplyToHandler
{
    private AjaxServletConnector connector;
    public AjaxReplyToHandler(List<Transformer> transformers, AjaxServletConnector connector)
    {
        super(transformers);
        this.connector = connector;
    }

    @Override
    public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
    {
        AbstractBayeux bayeux = connector.getBayeux();
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
            returnMessage.applyTransformers(getTransformers());
            ret = returnMessage.getPayload();
        }
        //Publish to interested clients
        for (Client client : channel.getSubscribers())
        {
            channel.publish(client, ret, null);
        }
    }
}
