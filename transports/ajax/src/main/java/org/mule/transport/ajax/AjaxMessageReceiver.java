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

import org.mule.RequestContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.ajax.embedded.AjaxConnector;
import org.mule.transport.ajax.i18n.AjaxMessages;
import org.mule.util.StringUtils;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.BayeuxService;

/**
 * Registers a receiver service with Bayeux.
 * The {@link AjaxMessageReceiver.ReceiverService#route(org.cometd.Client, Object)}
 * is invoked when a message is received on the subscription channel
 */
public class AjaxMessageReceiver extends AbstractMessageReceiver
{
    private AbstractBayeux bayeux;

    public AjaxMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        String path = endpoint.getEndpointURI().getPath();
        if(StringUtils.isEmpty(path) || path.equals("/"))
        {
            throw new CreateException(AjaxMessages.createStaticMessage("The subscription path cannotbe empty or equal '/'"), this);
        }
    }

    public class ReceiverService extends BayeuxService
    {
        private final ImmutableEndpoint endpoint;

        public ReceiverService(String channel, Bayeux bayeux, ImmutableEndpoint endpoint)
        {
            super(bayeux, channel);
            this.endpoint = endpoint;
            subscribe(channel, "route");
        }


        public Object route(Client client, Object data) throws Exception
        {
            AbstractConnector connector = (AbstractConnector) getConnector();
            MuleMessage messageToRoute = createMuleMessage(data, endpoint.getEncoding());
            messageToRoute.setInvocationProperty(AjaxConnector.COMETD_CIENT, client);

            Object replyTo = messageToRoute.getReplyTo();
            MuleMessage message = AjaxMessageReceiver.this.routeMessage(messageToRoute);
            //If a replyTo channel is set the client is expecting a response.
            //Mule does not invoke the replyTo handler if an error occurs, but in this case we want it to.
            if ((message != null && message.getExceptionPayload() == null) && replyTo != null)
            {
                connector.getReplyToHandler(endpoint).processReplyTo(RequestContext.getEvent(), message, replyTo);
            }
            return null;
        }
    }

    public AbstractBayeux getBayeux()
    {
        return bayeux;
    }

    public void setBayeux(AbstractBayeux bayeux)
    {
        this.bayeux = bayeux;
    }

    @Override
    protected void doStart() throws MuleException
    {
        //Register our listener service with Bayeux
        String channel = endpoint.getEndpointURI().getPath();
        new ReceiverService(channel, getBayeux(), getEndpoint());
    }
}

