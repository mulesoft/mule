/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.mule.tck.AbstractServiceAndFlowTestCase;

public abstract class AbstractMockHttpServerTestCase extends AbstractServiceAndFlowTestCase
{

    private static final long MOCK_HTTP_SERVER_STARTUP_TIMEOUT = 30000;
    private CountDownLatch serverStartLatch = new CountDownLatch(1);

    public AbstractMockHttpServerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        MockHttpServer httpServer = getHttpServer(serverStartLatch);
        new Thread(httpServer).start();

        // wait for the simple server thread to come up
        assertTrue("MockHttpServer start failed",
            serverStartLatch.await(MOCK_HTTP_SERVER_STARTUP_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    /**
     * Subclasses must implement this method to return their Subclass of
     * {@link MockHttpServer}.
     */
    protected abstract MockHttpServer getHttpServer(CountDownLatch latch);
}
