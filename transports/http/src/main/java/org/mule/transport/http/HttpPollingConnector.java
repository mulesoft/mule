/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
