/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file.issues;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.extras.client.MuleClient;
import org.mule.transport.file.AbstractFileFunctionalTestCase;

import java.io.File;

/**
 * This used to be part of FileFunctionalTest; moved here to allow isolation of individual case.
 */
public class IndirectReceiveMule1842TestCase extends AbstractFileFunctionalTestCase
{

    public void testIndirectRequest() throws Exception
    {
        File target = initForRequest();

        // add a receiver endpoint that will poll the readFromDirectory
        Service relay = muleContext.getRegistry().lookupService("relay");
        assertNotNull(relay);
        String url = fileToUrl(target) + "?connector=receiveConnector";
        logger.debug(url);
        
        ImmutableEndpoint endpoint = 
            muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(url);
        relay.getInboundRouter().addEndpoint(endpoint);
        relay.stop();
        relay.start();

        // then read from the queue that the polling receiver will write to
        MuleClient client = new MuleClient();
        MuleMessage message = client.request("receive", 3000);
        checkReceivedMessage(message);
    }

}
