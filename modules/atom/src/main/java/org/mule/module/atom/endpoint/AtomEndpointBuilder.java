/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.endpoint.AbstractMetaEndpointBuilder;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.http.HttpPollingConnector;
import org.mule.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An endpoint factory used for creating an ATOM endpoint
 */
public class AtomEndpointBuilder extends AbstractMetaEndpointBuilder
{
    public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
    public static final String SHORT_DATE_FORMAT = "yyyy-MM-dd";

    private boolean splitFeed = true;

    private String lastUpdate = null;

    private List<String> acceptedMimeTypes;

    private long pollingFrequency = 1000;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
    private final SimpleDateFormat shortDateFormatter = new SimpleDateFormat(SHORT_DATE_FORMAT);

    public AtomEndpointBuilder()
    {
        super();
        init();
    }

    public AtomEndpointBuilder(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);
        init();
    }

    public AtomEndpointBuilder(URIBuilder uriBuilder, MuleContext muleContext)
    {
        super(uriBuilder);
        init();
    }

    public AtomEndpointBuilder(String address, MuleContext muleContext)
    {
        super(address, muleContext);
        init();
    }

    protected AtomEndpointBuilder(EndpointURI endpointURI, MuleContext muleContext)
    {
        super(endpointURI);
        init();
    }

    protected void init()
    {

        acceptedMimeTypes = new ArrayList<String>();
        acceptedMimeTypes.add("application/atom+xml");
        acceptedMimeTypes.add("application/atom");
        acceptedMimeTypes.add("text/xml");
    }

    @Override
    public InboundEndpoint buildInboundEndpoint() throws EndpointException, InitialisationException
    {
        try
        {
            Date date = formatDate(getLastUpdate());
            AtomInboundEndpoint in = new AtomInboundEndpoint(isSplitFeed(), date, getAcceptedMimeTypes(), super.buildInboundEndpoint());
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
        throw new UnsupportedOperationException("Outbound ATOM endpoints not supported");
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

    public List<String> getAcceptedMimeTypes()
    {
        return acceptedMimeTypes;
    }

    public void setAcceptedMimeTypes(List<String> acceptedMimeTypes)
    {
        this.acceptedMimeTypes = acceptedMimeTypes;
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
