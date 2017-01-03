/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.request.grizzly;

import static com.google.common.net.MediaType.APPLICATION_XML_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.MuleException;
import org.mule.module.http.internal.ParameterMap;
import org.mule.module.http.internal.domain.request.DefaultHttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.request.HttpClientConfiguration;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class InputStreamSendHttpClientTest
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    private static final String EXPECTED_RESULT = "result";

    private static final String PROTOCOL = "http";

    private static final String HOST = "localhost";

    private static final String PATH = "retrieve";

    private static final int TIMEOUT = -1;

    private static final Boolean FOLLOW_REDIRECTS = Boolean.TRUE;

    private MockServer server = new MockServer(dynamicPort.getNumber());

    private GrizzlyHttpClient httpClient;
    
    @Before
    public void before() throws Exception
    {
        createClient();
    }
    @After
    public void after() throws Exception
    {
        server.stop();
        httpClient.stop();
    }

    @Test
    public void retrieveContentsFromInputStream() throws Exception
    {
        InputStream responseStream = null;

        HttpRequest request = new DefaultHttpRequest(
                PROTOCOL + "://" + HOST + ":" + dynamicPort.getNumber() + "/" + PATH, null, GET.asString(),
                new ParameterMap(), new ParameterMap(), null);
        responseStream = httpClient.sendAndReceiveInputStream(request, TIMEOUT, FOLLOW_REDIRECTS, null);

        String response = IOUtils.toString(responseStream, UTF_8.name());

        assertThat(EXPECTED_RESULT, equalTo(response));
    }

    private void createClient() throws MuleException
    {
        HttpClientConfiguration configuration = new HttpClientConfiguration.Builder().setUsePersistentConnections(true)
                                                                                     .setMaxConnections(1)
                                                                                     .setConnectionIdleTimeout(-1)
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

        public MockServer(int serverPort)
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
                    response.setContentType(APPLICATION_XML_UTF_8.toString());
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
