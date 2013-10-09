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
 * <code>HttpsPollingConnector</code> provides Secure http connectivity on top of what is already provided with the
 * Mule {@link org.mule.transport.http.HttpPollingConnector}.
 */
public class HttpsPollingConnector extends HttpsConnector
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

    public HttpsPollingConnector(MuleContext context)
    {
        super(context);
        serviceOverrides = new Properties();
        serviceOverrides.setProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS,
            PollingHttpsMessageReceiver.class.getName());
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
