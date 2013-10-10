/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ajax;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;
import org.mule.transport.ajax.i18n.AjaxMessages;
import org.mule.util.MapUtils;
import org.mule.util.StringUtils;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.cometd.Channel;
import org.cometd.Client;
import org.mortbay.cometd.AbstractBayeux;

/**
 * Will dispatch Mule events to ajax clients available in Bayeux that are listening to this endpoint.
 */
public class AjaxMessageDispatcher extends AbstractMessageDispatcher implements BayeuxAware
{
    protected AbstractBayeux bayeux;

    protected boolean cacheMessages = false;

    protected int messageCacheSize = 500;

    protected Buffer messageCache;

    protected String channel;

    protected Client client;

    public AjaxMessageDispatcher(OutboundEndpoint endpoint) throws CreateException
    {
        super(endpoint);
        cacheMessages = MapUtils.getBoolean(endpoint.getProperties(), "cacheMessages", false);
        messageCacheSize = MapUtils.getInteger(endpoint.getProperties(), "messageCacheSize", 500);
        channel = endpoint.getEndpointURI().getPath();
        if(StringUtils.isEmpty(channel) || channel.equals("/"))
        {
            //TODO i18n
            throw new CreateException(AjaxMessages.createStaticMessage("The subscription path cannot be empty or equal '/'"), this);
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
    protected void doInitialise() throws InitialisationException
    {
        if (cacheMessages)
        {
            messageCache = new BoundedFifoBuffer(messageCacheSize);
        }
    }

    protected Client getClient()
    {
        if(client == null)
        {
            client = bayeux.newClient(channel);
        }
        return client;
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        //We have no need for Null messages to be sent to the browser
        if(NullPayload.getInstance().equals(event.getMessage().getPayload()))
        {
            return;
        }
        if (!connector.isStarted())
        {
            //TODO MULE-4320
            logger.warn("Servlet container has not yet initialised, ignoring event: " + event.getMessage().getPayload());
            return;
        }

        Channel chan = bayeux.getChannel(channel);
        if(chan!=null)
        {
            if (chan.getSubscribers().size() > 0 && cacheMessages && !messageCache.isEmpty())
            {
                while (!messageCache.isEmpty())
                {
                    for (Client client : chan.getSubscribers())
                    {
                        deliver(client, channel, messageCache.remove());
                    }
                    //deliver(getClient(), channel, messageCache.remove());
                }
            }

            Object data = event.getMessage().getPayload();
            //deliver(getClient(), channel, data);
            for (Client client : chan.getSubscribers())
            {
                deliver(client, channel, data);
            }
        }
        else if (cacheMessages)
        {
            Object data = event.getMessage().getPayload();
            if (logger.isTraceEnabled())
            {
                logger.trace("There are no clients waiting, adding message to cache: " + data);
            }
            messageCache.add(data);
        }
    }

    protected void deliver(Client client, String channel, Object data)
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("Delivering to client id: " + client.getId() + " channel:" + channel);
        }
        client.deliver(client, channel, data, null);
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        doDispatch(event);
        return null;
    }

    @Override
    protected void doDispose()
    {
        if (messageCache != null)
        {
            messageCache.clear();
            messageCache = null;
        }
    }
}
