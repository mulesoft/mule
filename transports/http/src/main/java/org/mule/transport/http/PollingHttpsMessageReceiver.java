/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.util.MapUtils;

public class PollingHttpsMessageReceiver extends PollingHttpMessageReceiver
{
    public PollingHttpsMessageReceiver(Connector connector,
                                       FlowConstruct flowConstruct,
                                       InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    @Override
    protected void setupFromConnector(Connector connector) throws CreateException
    {
        if (!(connector instanceof HttpsPollingConnector))
        {
            throw new CreateException(HttpMessages.pollingReciverCannotbeUsed(), this);
        }

        HttpsPollingConnector pollingConnector = (HttpsPollingConnector) connector;
        long pollingFrequency = MapUtils.getLongValue(endpoint.getProperties(), "pollingFrequency",
                pollingConnector.getPollingFrequency());
        if (pollingFrequency > 0)
        {
            setFrequency(pollingFrequency);
        }

        checkEtag = MapUtils.getBooleanValue(endpoint.getProperties(), "checkEtag",
            pollingConnector.isCheckEtag());
        discardEmptyContent = MapUtils.getBooleanValue(endpoint.getProperties(),
            "discardEmptyContent", pollingConnector.isDiscardEmptyContent());
    }
}


