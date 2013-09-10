/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file.issues;

import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.service.Service;
import org.mule.api.source.CompositeMessageSource;
import org.mule.construct.Flow;
import org.mule.transport.file.AbstractFileFunctionalTestCase;

import java.io.File;

import org.junit.Test;

/**
 * This used to be part of FileFunctionalTest; moved here to allow isolation of
 * individual case.
 */
public class IndirectReceiveMule1842TestCase extends AbstractFileFunctionalTestCase
{
    public IndirectReceiveMule1842TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testIndirectRequest() throws Exception
    {
        File target = initForRequest();

        // add a receiver endpoint that will poll the readFromDirectory
        Object relay = muleContext.getRegistry().lookupObject("relay");
        assertNotNull(relay);
        String url = fileToUrl(target) + "?connector=receiveConnector";
        logger.debug(url);

        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(url);

        if (variant.equals(ConfigVariant.FLOW))
        {
            ((CompositeMessageSource) ((Flow) relay).getMessageSource()).addSource(endpoint);
        }
        else
        {
            ((CompositeMessageSource) ((Service) relay).getMessageSource()).addSource(endpoint);
        }

        ((Stoppable) relay).stop();
        ((Startable) relay).start();

        // then read from the queue that the polling receiver will write to
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request("receive", 3000);
        checkReceivedMessage(message);
    }
}
