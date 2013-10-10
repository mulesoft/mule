/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
