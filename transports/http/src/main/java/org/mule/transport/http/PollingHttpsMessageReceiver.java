/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


