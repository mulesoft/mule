/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.components;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.functional.AbstractMockHttpServerTestCase;
import org.mule.transport.http.functional.MockHttpServer;
import org.mule.transport.http.functional.SingleRequestMockHttpServer;

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
    protected MockHttpServer getHttpServer(CountDownLatch serverStartLatch)
    {
        return new SimpleHttpServer(dynamicPort.getNumber(), serverStartLatch, serverRequestCompleteLatch);
    }

    @Test
    public void testRestServiceComponentDelete() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.send("vm://fromTest", TEST_MESSAGE, null);

        assertTrue(serverRequestCompleteLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(deleteRequestFound);
    }

    private class SimpleHttpServer extends SingleRequestMockHttpServer
    {
        public SimpleHttpServer(int listenPort, CountDownLatch startupLatch, CountDownLatch testCompleteLatch)
        {
            super(listenPort, startupLatch, testCompleteLatch);
        }

        @Override
        protected void readHttpRequest(BufferedReader reader) throws Exception
        {
            String requestLine = reader.readLine();
            String httpMethod = new StringTokenizer(requestLine).nextToken();

            deleteRequestFound = httpMethod.equals(HttpConstants.METHOD_DELETE);
        }
    }
}
