/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;

import java.util.Properties;

/**
 * The HttpPollingConnectors allows for inbound Http endpoints to be configured with an address which it shall use
 * to poll for a result. If a result is received it becomes the inbound event for the component.  This connector is
 * useful for interacting with services that provide pull-only support for obtaining data.  This is typical for many
 * web-based services.
 */
public class HttpPollingConnector extends HttpConnector
{
    /**
     * How long to wait in milliseconds between make a new request
     */
    private long pollingFrequency = 1000L;

    /**
     * If a zero-length content is returned should the message be discarded
     */
    private boolean discardEmptyContent = true;

    /**
     * Should the ETag header get honoured if it is present.
     */
    private boolean checkEtag = true;

    public HttpPollingConnector(MuleContext context)
    {
        super(context);
        serviceOverrides = new Properties();
        serviceOverrides.setProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS, PollingHttpMessageReceiver.class.getName());
    }

    public boolean isDiscardEmptyContent()
    {
        return discardEmptyContent;
    }

    public void setDiscardEmptyContent(boolean discardEmptyContent)
    {
        this.discardEmptyContent = discardEmptyContent;
    }

    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }

    public boolean isCheckEtag()
    {
        return checkEtag;
    }

    public void setCheckEtag(boolean checkEtag)
    {
        this.checkEtag = checkEtag;
    }
}
