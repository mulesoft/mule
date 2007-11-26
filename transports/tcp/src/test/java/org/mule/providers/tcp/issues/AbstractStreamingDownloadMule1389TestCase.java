/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

import org.mule.extras.client.MuleClient;
import org.mule.providers.tcp.integration.AbstractStreamingCapacityTestCase;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public abstract class AbstractStreamingDownloadMule1389TestCase extends FunctionalTestCase
{

    protected String endpoint;

    public AbstractStreamingDownloadMule1389TestCase(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public void testDownloadSpeed() throws Exception
    {
        MuleClient client = new MuleClient();
        long now = System.currentTimeMillis();
        UMOMessage result = client.send(endpoint, "request", null);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertEquals(InputStreamSource.SIZE, result.getPayloadAsBytes().length);
        long then = System.currentTimeMillis();
        double speed = InputStreamSource.SIZE / (double) (then - now) * 1000 / AbstractStreamingCapacityTestCase.ONE_MB;
        logger.error("Transfer speed " + speed + " MB/s (" + InputStreamSource.SIZE + " B in " + (then - now) + " ms)");
    }

}
