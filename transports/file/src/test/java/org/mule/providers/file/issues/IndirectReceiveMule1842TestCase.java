/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file.issues;

import org.mule.extras.client.MuleClient;
import org.mule.providers.file.AbstractFileFunctionalTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

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
        UMOComponent relay = managementContext.getRegistry().lookupComponent("relay");
        assertNotNull(relay);
        String url = fileToUrl(target) + "?connector=receiveConnector";
        logger.debug(url);
        
        UMOImmutableEndpoint endpoint = 
            managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(url);
        relay.getInboundRouter().addEndpoint(endpoint);
        relay.stop();
        relay.start();

        // then read from the queue that the polling receiver will write to
        MuleClient client = new MuleClient();
        UMOMessage message = client.request("receive", 3000);
        checkReceivedMessage(message);
    }

}
