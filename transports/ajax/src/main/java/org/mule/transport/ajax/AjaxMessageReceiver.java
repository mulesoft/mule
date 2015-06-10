/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.PropertyScope;
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
public class AjaxMessageReceiver extends AbstractMessageReceiver implements BayeuxAware
{
    private AbstractBayeux bayeux;

    public AjaxMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        String channel = endpoint.getEndpointURI().getPath();
        if(StringUtils.isEmpty(channel) || channel.equals("/"))
        {
            //TODO i18n
            throw new CreateException(AjaxMessages.createStaticMessage("The subscription path cannot be empty or equal '/'"), this);
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
            MuleMessage messageToRoute = createMuleMessage(data, endpoint.getEncoding());
            messageToRoute.setInvocationProperty(AjaxConnector.COMETD_CLIENT, client);

            Object replyTo = messageToRoute.getReplyTo();
            if (replyTo != null)
            {
                messageToRoute.setProperty(MuleProperties.MULE_FORCE_SYNC_PROPERTY, Boolean.TRUE, PropertyScope.INBOUND);
            }

            MuleEvent event = AjaxMessageReceiver.this.routeMessage(messageToRoute);
            MuleMessage message = event == null ? null : event.getMessage();

            // only the AjaxConnector (as opposed to the AjaxServletConnector) has the
            // isDisableReplyTo() method and both inherit from different superclasses
            if (getConnector() instanceof AjaxConnector)
            {
                // If a replyTo channel is set the client is expecting a response.
                // Mule does not invoke the replyTo handler if an error occurs, but in this case we
                // want it to.
                AjaxConnector ajaxConnector = (AjaxConnector) getConnector();
                if (!ajaxConnector.isDisableReplyTo() && message != null && message.getExceptionPayload() == null && replyTo != null && endpoint.getExchangePattern().hasResponse())
                {
                    ajaxConnector.getReplyToHandler(endpoint).processReplyTo(RequestContext.getEvent(), message, replyTo);
                }
            }
            return null;
        }
    }

    @Override
    public AbstractBayeux getBayeux()
    {
        return bayeux;
    }

    @Override
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
