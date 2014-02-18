/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax.container;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.ReplyToHandler;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ajax.AjaxMessageReceiver;
import org.mule.transport.ajax.AjaxReplyToHandler;
import org.mule.transport.ajax.BayeuxAware;
import org.mule.transport.ajax.embedded.AjaxConnector;
import org.mule.transport.servlet.ServletConnector;

import org.cometd.DataFilter;
import org.mortbay.cometd.AbstractBayeux;

/**
 * A servlet connector that binds to the container and makes a configured
 * Bayeux available to dispatchers and receivers.
 */
public class AjaxServletConnector extends ServletConnector implements BayeuxAware
{
    public static final String PROTOCOL = "ajax-servlet";

    /**
     * The client side poll timeout in milliseconds (default 0). How long a client
     * will wait between reconnects
     */
    private int interval = INT_VALUE_NOT_SET;

    /**
     * The max client side poll timeout in milliseconds (default 30000). A client
     * will be removed if a connection is not received in this time.
     */
    private int maxInterval = INT_VALUE_NOT_SET;

    /**
     * The client side poll timeout if multiple connections are detected from the
     * same browser (default 1500).
     */
    private int multiFrameInterval = INT_VALUE_NOT_SET;

    /**
     * 0=none, 1=info, 2=debug
     */
    private int logLevel = INT_VALUE_NOT_SET;

    /**
     * The server side poll timeout in milliseconds (default 250000). This is how long
     * the server will hold a reconnect request before responding.
     */
    private int timeout = INT_VALUE_NOT_SET;

    /**
     * If "true" (default) then the server will accept JSON wrapped in a comment and
     * will generate JSON wrapped in a comment. This is a defence against Ajax Hijacking.
     */
    private boolean jsonCommented = true;

    /**
     * TODO SUPPORT FILTERS
     * the location of a JSON file describing {@link DataFilter} instances to be installed
     */
    private String filters;

    /**
     * If true, the current request is made available via the
     * {@link AbstractBayeux#getCurrentRequest()} method
     */
    private boolean requestAvailable = true;

    /**
     * The number of message refs at which the a single message response will be
     * cached instead of being generated for every client delivered to. Done to optimize
     * a single message being sent to multiple clients.
     */
    private int refsThreshold = INT_VALUE_NOT_SET;


    protected AbstractBayeux bayeux;

    public AjaxServletConnector(MuleContext context)
    {
        super(context);
        registerSupportedProtocolWithoutPrefix(AjaxConnector.PROTOCOL);
        //Dont start until the servletContainer is up
        setInitialStateStopped(true);
    }

    @Override
    public AbstractBayeux getBayeux()
    {
        return bayeux;
    }

    @Override
    public void setBayeux(AbstractBayeux bayeux) throws MuleException
    {
        this.bayeux = bayeux;
        this.getBayeux().setJSONCommented(isJsonCommented());
        if(getLogLevel() != AbstractConnector.INT_VALUE_NOT_SET) this.getBayeux().setLogLevel(getLogLevel());
        if(getMaxInterval() != AbstractConnector.INT_VALUE_NOT_SET) this.getBayeux().setMaxInterval(getMaxInterval());
        if(getInterval() != AbstractConnector.INT_VALUE_NOT_SET) this.getBayeux().setInterval(getMaxInterval());
        if(getMultiFrameInterval() != AbstractConnector.INT_VALUE_NOT_SET) this.getBayeux().setMultiFrameInterval(getMultiFrameInterval());
        if(getTimeout() != AbstractConnector.INT_VALUE_NOT_SET) this.getBayeux().setTimeout(getMultiFrameInterval());
        //Only start once we have this
        this.setInitialStateStopped(false);

        for (Object receiver : receivers.values())
        {
            ((AjaxMessageReceiver)receiver).setBayeux(getBayeux());
        }
        start();
    }

    @Override
    public String getProtocol()
    {
        return PROTOCOL;
    }

    public int getInterval()
    {
        return interval;
    }

    public void setInterval(int interval)
    {
        this.interval = interval;
    }

    public int getMaxInterval()
    {
        return maxInterval;
    }

    public void setMaxInterval(int maxInterval)
    {
        this.maxInterval = maxInterval;
    }

    public int getMultiFrameInterval()
    {
        return multiFrameInterval;
    }

    public void setMultiFrameInterval(int multiFrameInterval)
    {
        this.multiFrameInterval = multiFrameInterval;
    }

    public int getLogLevel()
    {
        return logLevel;
    }

    public void setLogLevel(int logLevel)
    {
        this.logLevel = logLevel;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public boolean isJsonCommented()
    {
        return jsonCommented;
    }

    public void setJsonCommented(boolean jsonCommented)
    {
        this.jsonCommented = jsonCommented;
    }

    public String getFilters()
    {
        return filters;
    }

    public void setFilters(String filters)
    {
        this.filters = filters;
    }

    public boolean isRequestAvailable()
    {
        return requestAvailable;
    }

    public void setRequestAvailable(boolean requestAvailable)
    {
        this.requestAvailable = requestAvailable;
    }

    public int getRefsThreshold()
    {
        return refsThreshold;
    }

    public void setRefsThreshold(int refsThreshold)
    {
        this.refsThreshold = refsThreshold;
    }

    @Override
    public ReplyToHandler getReplyToHandler(ImmutableEndpoint endpoint)
    {
        return new AjaxReplyToHandler(this, endpoint.getMuleContext());
    }

    @Override
    protected MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
    {
        AjaxMessageReceiver receiver = (AjaxMessageReceiver) super.createReceiver(flowConstruct, endpoint);
        //The Bayeux object will be null of the connector has not started yet, nothing to worry about
        receiver.setBayeux(getBayeux());
        return receiver;
    }
}
