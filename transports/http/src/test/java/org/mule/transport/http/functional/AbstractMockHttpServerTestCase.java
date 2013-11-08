/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertTrue;
import org.mule.tck.AbstractServiceAndFlowTestCase;

public abstract class AbstractMockHttpServerTestCase extends AbstractServiceAndFlowTestCase
{

    private MockHttpServer mockHttpServer;

    public AbstractMockHttpServerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        mockHttpServer = getHttpServer();
        new Thread(mockHttpServer).start();

        assertTrue("MockHttpServer start failed", mockHttpServer.waitUntilServerStarted());
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();

        assertTrue("MockHttpServer failed to shut down", mockHttpServer.waitUntilServerStopped());
    }

    /**
     * Subclasses must implement this method to return their Subclass of
     * {@link MockHttpServer}.
     */
    protected abstract MockHttpServer getHttpServer();
}
