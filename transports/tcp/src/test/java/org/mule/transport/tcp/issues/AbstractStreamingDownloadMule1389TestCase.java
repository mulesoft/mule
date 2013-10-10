/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
