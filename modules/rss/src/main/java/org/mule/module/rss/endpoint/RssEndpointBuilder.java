/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.filter.Filter;
import org.mule.endpoint.AbstractMetaEndpointBuilder;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.module.rss.routing.EntryLastUpdatedFilter;
import org.mule.module.rss.routing.FeedSplitter;
import org.mule.transport.http.HttpPollingConnector;
import org.mule.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Creates RSS endpoints. Right now only inbound endpoints are supported, i.e. poll an RSS URL
 */
public class RssEndpointBuilder extends AbstractMetaEndpointBuilder
{
    public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
    public static final String SHORT_DATE_FORMAT = "yyyy-MM-dd";

    private boolean splitFeed = true;

    private String lastUpdate = null;

    private long pollingFrequency = 1000;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
    private final SimpleDateFormat shortDateFormatter = new SimpleDateFormat(SHORT_DATE_FORMAT);

    public RssEndpointBuilder()
    {
        super();
        init();
    }

    public RssEndpointBuilder(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);
        init();
    }

    public RssEndpointBuilder(URIBuilder URIBuilder)
    {
        super(URIBuilder);
        init();
    }

    public RssEndpointBuilder(String address, MuleContext muleContext)
    {
        super(address, muleContext);
        init();
    }

    protected RssEndpointBuilder(EndpointURI endpointURI)
    {
        super(endpointURI);
        init();
    }

    protected void init()
    {
        shortDateFormatter.setLenient(false);
        dateFormatter.setLenient(false);
    }

    @Override
    public InboundEndpoint buildInboundEndpoint() throws EndpointException, InitialisationException
    {
        try
        {
            Date date = formatDate(getLastUpdate());
            if (isSplitFeed())
            {
                Filter filter = new EntryLastUpdatedFilter(date);
                FeedSplitter splitter = new FeedSplitter();
                splitter.setEntryFilter(filter);
                addMessageProcessor(splitter);

            }
            RssInboundEndpoint in = new RssInboundEndpoint(isSplitFeed(), date, super.buildInboundEndpoint());
            in.registerSupportedProtocol("http");
            in.registerSupportedProtocol("https");
            in.registerSupportedProtocol("vm");
            if (in.getConnector() instanceof HttpPollingConnector)
            {
                ((HttpPollingConnector) in.getConnector()).setPollingFrequency(pollingFrequency);
            }
            return in;
        }
        catch (ParseException e)
        {
            throw new EndpointException(e);
        }
    }

    @Override
    public OutboundEndpoint buildOutboundEndpoint() throws EndpointException, InitialisationException
    {
        throw new UnsupportedOperationException("Outbound RSS endpoints not supported");
    }

    @Override
    protected boolean isAlwaysCreateConnector()
    {
        return true;
    }

    public String getLastUpdate()
    {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate)
    {
        this.lastUpdate = lastUpdate;
    }

    public boolean isSplitFeed()
    {
        return splitFeed;
    }

    public void setSplitFeed(boolean splitFeed)
    {
        this.splitFeed = splitFeed;
    }

    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }

    protected Date formatDate(String date) throws ParseException
    {
        Date lastUpdateDate = null;
        if (StringUtils.isNotBlank(date))
        {
            if (lastUpdate.length() == 10)
            {
                lastUpdateDate = shortDateFormatter.parse(date);
            }
            else
            {
                lastUpdateDate = dateFormatter.parse(date);
            }
        }
        return lastUpdateDate;
    }
}
