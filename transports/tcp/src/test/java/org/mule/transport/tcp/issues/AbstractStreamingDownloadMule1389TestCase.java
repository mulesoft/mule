/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.tcp.integration.AbstractStreamingCapacityTestCase;

public abstract class AbstractStreamingDownloadMule1389TestCase extends AbstractServiceAndFlowTestCase
{    
    public AbstractStreamingDownloadMule1389TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testDownloadSpeed() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        long now = System.currentTimeMillis();
        MuleMessage result = client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inTestComponent")).getAddress(),
            "request", null);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertEquals(InputStreamSource.SIZE, result.getPayloadAsBytes().length);
        long then = System.currentTimeMillis();
        double speed = InputStreamSource.SIZE / (double) (then - now) * 1000 / AbstractStreamingCapacityTestCase.ONE_MB;
        logger.info("Transfer speed " + speed + " MB/s (" + InputStreamSource.SIZE + " B in " + (then - now) + " ms)");
    }

}
