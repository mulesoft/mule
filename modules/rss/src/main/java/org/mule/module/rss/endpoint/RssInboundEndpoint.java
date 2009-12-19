/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss.endpoint;

import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.InboundEndpointDecorator;
import org.mule.api.routing.filter.Filter;
import org.mule.api.service.Service;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.module.rss.routing.EntryLastUpdatedFilter;
import org.mule.module.rss.routing.InboundFeedSplitter;
import org.mule.module.rss.transformers.ObjectToRssFeed;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO
 */
public class RssInboundEndpoint extends DefaultInboundEndpoint implements InboundEndpointDecorator
{
    private boolean splitFeed;

    private Date lastUpdate;

    private List<String> acceptedMimeTypes;

    private ObjectToRssFeed inTransform = new ObjectToRssFeed();

    private Set<String> supportedProtocols = new HashSet<String>(2);

    public RssInboundEndpoint(boolean splitFeed, Date lastUpdate, List<String> acceptedContentTypes, InboundEndpoint ie)
    {
        super(ie.getConnector(), ie.getEndpointURI(), ie.getTransformers(), ie.getResponseTransformers(), ie.getName(),
                ie.getProperties(), ie.getTransactionConfig(), ie.getFilter(), ie.isDeleteUnacceptedMessages(),
                ie.getSecurityFilter(), ie.isSynchronous(), ie.getResponseTimeout(), ie.getInitialState(),
                ie.getEncoding(), ie.getEndpointBuilderName(), ie.getMuleContext(), ie.getRetryPolicyTemplate());
        this.splitFeed = splitFeed;
        this.lastUpdate = lastUpdate;
        this.acceptedMimeTypes = acceptedContentTypes;
    }

    public boolean isSplitFeed()
    {
        return splitFeed;
    }

    public Date getLastUpdate()
    {
        return lastUpdate;
    }

    public List<String> getAcceptedMimeTypes()
    {
        return acceptedMimeTypes;
    }

    void registerSupportedProtocol(String protocol)
    {
        supportedProtocols.add(protocol);
    }

    boolean unregisterProtocol(String protocol)
    {
        return supportedProtocols.remove(protocol);
    }

    public boolean isProtocolSupported(String protocol)
    {
        return supportedProtocols.contains(protocol);
    }


    public void onListenerAdded(Service service) throws MuleException
    {
        if (isSplitFeed())
        {
            Filter filter = new EntryLastUpdatedFilter(getLastUpdate());
            InboundFeedSplitter splitter = new InboundFeedSplitter();
            splitter.setEntryFilter(filter);
            splitter.setMuleContext(getMuleContext());
            splitter.setAcceptedContentTypes(getAcceptedMimeTypes());
            splitter.initialise();
            service.getInboundRouter().addRouter(splitter);
        }
    }

    public boolean onMessage(MuleMessage message) throws MuleException
    {
        String mimeType = getMime(message);

        if (getProtocol().startsWith("http") && !isMimeSupported(mimeType))
        {
            if (mimeType == null)
            {
                throw new MessagingException(CoreMessages.createStaticMessage("Mime type not set on message, cannot validate that message is an AtomInboundEndpointFactoryBean feed"), message);
            }
            else
            {
                throw new MessagingException(CoreMessages.createStaticMessage("Mime type not supported '" + mimeType + "', supported types are: " + getAcceptedMimeTypes()), message);
            }
        }
        else if (mimeType == null)
        {
            logger.warn("Mime type not set on message, but connector protocol '" + getProtocol() + "' does not explicitly support mimeTypes. Message type will not be validated");
        }
        message.applyTransformers(inTransform);
        return true;
    }


    public boolean isMimeSupported(String mime)
    {
        return acceptedMimeTypes.contains(mime);
    }

    private String getMime(MuleMessage m)
    {
        String mimeType = m.getStringProperty("Content-Type", null);
        if (m == null)
        {
            return null;
        }
        int i = mimeType.indexOf(";");
        if (i > -1)
        {
            mimeType = mimeType.substring(0, i);
        }
        return mimeType;
    }
}
