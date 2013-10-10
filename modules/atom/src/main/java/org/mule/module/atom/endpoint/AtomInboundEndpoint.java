/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.endpoint;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.module.atom.transformers.ObjectToFeed;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * An ATOM endpoint is used for polling an ATOM feed and processing the feed entries
 */
public class AtomInboundEndpoint extends DefaultInboundEndpoint 
{
    private boolean splitFeed;

    private Date lastUpdate;

    private ObjectToFeed inTransform = new ObjectToFeed();

    private Set<String> supportedProtocols = new HashSet<String>(2);

    public AtomInboundEndpoint(boolean splitFeed, Date lastUpdate, InboundEndpoint ie)
    {
        super(ie.getConnector(), ie.getEndpointURI(), ie.getName(),
                ie.getProperties(), ie.getTransactionConfig(), ie.isDeleteUnacceptedMessages(),
                ie.getExchangePattern(), ie.getResponseTimeout(), ie.getInitialState(),
                ie.getEncoding(), ie.getEndpointBuilderName(), ie.getMuleContext(), ie.getRetryPolicyTemplate(), 
                ie.getMessageProcessorsFactory(), ie.getMessageProcessors(), ie.getResponseMessageProcessors(), ie.isDisableTransportTransformer(), ie.getMimeType());
        this.splitFeed = splitFeed;
        this.lastUpdate = lastUpdate;
    }

    public boolean isSplitFeed()
    {
        return splitFeed;
    }

    public Date getLastUpdate()
    {
        return lastUpdate;
    }

    void registerSupportedProtocol(String protocol)
    {
        supportedProtocols.add(protocol);
    }

    boolean unregisterProtocol(String protocol)
    {
        return supportedProtocols.remove(protocol);
    }

    @Override
    public boolean isProtocolSupported(String protocol)
    {
        return supportedProtocols.contains(protocol);
    }

    public boolean onMessage(MuleMessage message) throws MuleException
    {
        message.applyTransformers(null, inTransform);
        return true;
    }
}
