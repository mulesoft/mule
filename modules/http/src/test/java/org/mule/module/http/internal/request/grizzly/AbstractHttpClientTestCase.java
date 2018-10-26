/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.request.grizzly;

import static com.google.common.net.MediaType.APPLICATION_XML_UTF_8;

import java.io.IOException;
import java.io.InputStream;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mule.api.MuleException;
import org.mule.module.http.internal.ParameterMap;
import org.mule.module.http.internal.domain.HttpEntity;
import org.mule.module.http.internal.domain.request.DefaultHttpRequest;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.request.HttpClientConfiguration;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.ssl.api.TlsContextFactoryBuilder;

public abstract class AbstractHttpClientTestCase extends AbstractMuleContextTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    protected static final String EXPECTED_RESULT = "result";

    private static final String PROTOCOL = "http";

    private static final String HOST = "localhost";

    private static final String PATH = "retrieve";

    protected static final int TIMEOUT = -1;

    protected static final Boolean FOLLOW_REDIRECTS = Boolean.TRUE;

    private MockServer server = new MockServer(dynamicPort.getNumber());

    protected TestGrizzlyHttpClient httpClient;
    
    @Before
    public void before() throws Exception
    {
        createClient();
        server.start();
    }
    @After
    public void after() throws Exception
    {
        server.stop();
        httpClient.stop();
    }

    protected void createClient() throws MuleException
    {
        TlsContextFactory defaultTlsContextFactory = new TlsContextFactoryBuilder(muleContext).buildDefault();
        HttpClientConfiguration configuration = new HttpClientConfiguration.Builder().setUsePersistentConnections(true)
                                                                                     .setDefaultTlsContextFactory(defaultTlsContextFactory)
                                                                                     .setMaxConnections(1)
                                                                                     .setStreaming(false)
                                                                                     .setConnectionIdleTimeout(-1)
                                                                                     .build();
        httpClient = new TestGrizzlyHttpClient(configuration);
        httpClient.start();
    }

    protected DefaultHttpRequest createRequest(HttpEntity entity, String method)
    {
        return new DefaultHttpRequest(
                PROTOCOL + "://" + HOST + ":" + dynamicPort.getNumber() + "/" + PATH, null, method,
                new ParameterMap(), new ParameterMap(), entity);
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
            this.serverPort = serverPort;
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
    
    class TestGrizzlyHttpClient extends GrizzlyHttpClient
    {

        private boolean mustAsyncRetrieveHeadersInResponse;
        
        public TestGrizzlyHttpClient(HttpClientConfiguration config)
        {
            super(config);
        }
        
        @Override
        protected HttpResponse createMuleResponse(final com.ning.http.client.Response response, InputStream inputStream) throws IOException
        {
            if (mustAsyncRetrieveHeadersInResponse)
            {
                Thread thread = new Thread()
                {

                    @Override
                    public void run()
                    {
                        response.getHeaders().keySet();
                    }
                };
                thread.start();
            }

            return super.createMuleResponse(response, inputStream);
        }
        
        public void setMustAsyncRetrieveHeadersInResponse(boolean mustAsyncRetrieveHeadersInResponse)
        {
            this.mustAsyncRetrieveHeadersInResponse = mustAsyncRetrieveHeadersInResponse;
        }
    }

}
