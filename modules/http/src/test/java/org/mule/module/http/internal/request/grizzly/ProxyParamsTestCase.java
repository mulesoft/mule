/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.request.grizzly;

import org.apache.commons.io.IOUtils;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.api.MuleException;
import org.mule.module.http.internal.ParameterMap;
import org.mule.module.http.internal.domain.request.DefaultHttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.request.DefaultProxyConfig;
import org.mule.module.http.internal.request.HttpClientConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class ProxyParamsTestCase extends AbstractMuleTestCase
{

    public DynamicPort dynamicPort = new DynamicPort("port");
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    private static final String EXPECTED_RESULT = "result";
    private static final String PROTOCOL = "http";
    private static final String HOST = "localhost";
    private static final String PATH = "retrieve";
    private static final int TIMEOUT = -1;
    private static final Boolean FOLLOW_REDIRECTS = Boolean.TRUE;
    private MockServer server = new MockServer(dynamicPort.getNumber());
    private GrizzlyHttpClient httpClient;

    @Parameterized.Parameter()
    public String nonProxyHosts;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {""},
                {"localhost"},
                {"other|localhost"}
        });
    }
    
    @After
    public void after() throws Exception
    {
        server.stop();
        httpClient.stop();
    }

    @Test
    public void testProxyNonProxyParams() throws Exception {
        createProxyAndClient(nonProxyHosts);

        final HttpRequest request = new DefaultHttpRequest(
                PROTOCOL + "://" + HOST + ":" + dynamicPort.getNumber() + "/" + PATH, null, GET.asString(),
                new ParameterMap(), new ParameterMap(), null);

        if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
            // as we set the nonProxyHosts to localhost, the proxy will not block this request
            InputStream responseStream = httpClient.sendAndReceiveInputStream(request, TIMEOUT, FOLLOW_REDIRECTS, null);
            assertNotNull(responseStream);
            assertEquals(EXPECTED_RESULT, IOUtils.toString(responseStream));
        } else {
            try {
                httpClient.sendAndReceiveInputStream(request, TIMEOUT, FOLLOW_REDIRECTS, null);
                fail("Request must be blocked by proxy when nonProxyHosts are configured");
            } catch (Exception ignore) {}
        }
    }

    private void createProxyAndClient(final String nonProxyHosts) throws MuleException
    {
        // create proxy config
        DefaultProxyConfig defaultProxyConfig = new DefaultProxyConfig();
        defaultProxyConfig.setName("testProxy");
        defaultProxyConfig.setHost(HOST);
        defaultProxyConfig.setPort(proxyPort.getNumber());
        if (nonProxyHosts != null) defaultProxyConfig.setNonProxyHosts(nonProxyHosts);

        HttpClientConfiguration configuration = new HttpClientConfiguration.Builder().setUsePersistentConnections(true)
                                                                                     .setMaxConnections(1)
                                                                                     .setStreaming(false)
                                                                                     .setConnectionIdleTimeout(-1)
                                                                                     .setProxyConfig(defaultProxyConfig)
                                                                                     .build();

        httpClient = new GrizzlyHttpClient(configuration);
        httpClient.start();
    }

    /**
     * Implementation of an http fake server
     */
    private static class MockServer
    {

        private int serverPort;
        private HttpServer server;

        MockServer(int serverPort)
        {
            try
            {
                this.serverPort = serverPort;
                start();
            }
            catch (Exception e)
            {
                fail("Could not construct mock server");
            }
        }

        public void start() throws IOException
        {
            server = HttpServer.createSimpleServer("/", serverPort);
            server.getServerConfiguration().addHttpHandler(new HttpHandler()
            {
                public void service(Request request, Response response) throws Exception
                {
//                    response.setContentType(APPLICATION_XML_UTF_8.toString());
                    String contents = EXPECTED_RESULT;
                    response.setContentLength(contents.length());
                    response.getWriter().write(contents);
                }
            }, "/" + PATH);

            server.start();
        }

        public void stop() throws Exception
        {
            server.shutdownNow();
        }

    }

}
