/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.transport.tcp.integration.AbstractStreamingCapacityTestCase;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public abstract class AbstractStreamingDownloadMule1389TestCase extends FunctionalTestCase {

  @Test
  public void testDownloadSpeed() throws Exception {
    MuleClient client = muleContext.getClient();
    long now = System.currentTimeMillis();
    MuleMessage result =
        client.send(((InboundEndpoint) muleContext.getRegistry().lookupObject("inTestComponent")).getAddress(), "request", null)
            .getRight();
    assertNotNull(result);
    assertNotNull(result.getPayload());
    assertEquals(InputStreamSource.SIZE, getPayloadAsBytes(result).length);
    long then = System.currentTimeMillis();
    double speed = InputStreamSource.SIZE / (double) (then - now) * 1000 / AbstractStreamingCapacityTestCase.ONE_MB;
    logger.info("Transfer speed " + speed + " MB/s (" + InputStreamSource.SIZE + " B in " + (then - now) + " ms)");
  }
}
