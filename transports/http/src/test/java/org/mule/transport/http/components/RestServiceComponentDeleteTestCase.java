/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.components;

import static org.junit.Assert.assertTrue;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpRequest;
import org.mule.transport.http.functional.AbstractMockHttpServerTestCase;
import org.mule.transport.http.functional.MockHttpServer;
import org.mule.transport.http.functional.SingleRequestMockHttpServer;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class RestServiceComponentDeleteTestCase extends AbstractMockHttpServerTestCase
{
    private CountDownLatch serverRequestCompleteLatch = new CountDownLatch(1);
    private boolean deleteRequestFound = false;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public RestServiceComponentDeleteTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "rest-service-component-delete-test-service.xml"},
            {ConfigVariant.FLOW, "rest-service-component-delete-test-flow.xml"}});
    }

    @Override
    protected MockHttpServer getHttpServer()
    {
        return new SimpleHttpServer(dynamicPort.getNumber());
    }

    @Test
    public void testRestServiceComponentDelete() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://fromTest", TEST_MESSAGE, null);

        assertTrue(serverRequestCompleteLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(deleteRequestFound);
    }

    private class SimpleHttpServer extends SingleRequestMockHttpServer
    {
        public SimpleHttpServer(int listenPort)
        {
            super(listenPort, muleContext.getConfiguration().getDefaultEncoding());
        }

        @Override
        protected void processSingleRequest(HttpRequest httpRequest) throws Exception
        {
            deleteRequestFound = httpRequest.getRequestLine().getMethod().equals(HttpConstants.METHOD_DELETE);
            serverRequestCompleteLatch.countDown();
        }
    }
}
