/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.proxy;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.functional.TestInputStream;
import org.mule.module.http.functional.requester.AbstractHttpRequestTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;
import org.mule.util.concurrent.Latch;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class HttpProxyTemplateTestCase extends AbstractHttpRequestTestCase
{

    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    private RequestHandlerExtender handlerExtender;
    private boolean consumeAllRequest = true;

    @Override
    protected String getConfigFile()
    {
        return "http-proxy-template-config.xml";
    }

    @Test
    public void proxySimpleRequests() throws Exception
    {
        handlerExtender = null;
        assertRequestOk(getProxyUrl(""), null);
        assertRequestOk(getProxyUrl("test"), null);
    }

    @Test
    public void proxyMethod() throws Exception
    {
        handlerExtender = new EchoRequestHandlerExtender()
        {
            @Override
            protected String selectRequestPartToReturn(org.eclipse.jetty.server.Request baseRequest)
            {
                return baseRequest.getMethod();
            }
        };
        assertRequestOk(getProxyUrl("test?parameterName=parameterValue"), "GET");

        Response response = Request.Post(getProxyUrl("test?parameterName=parameterValue"))
                .bodyString("Some Text", ContentType.DEFAULT_TEXT)
                .connectTimeout(1000).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is("POST"));
    }

    @Ignore
    @Test
    public void proxyProtocolHttp1_0() throws Exception
    {
        handlerExtender = new EchoRequestHandlerExtender()
        {
            @Override
            protected String selectRequestPartToReturn(org.eclipse.jetty.server.Request baseRequest)
            {
                return baseRequest.getProtocol();
            }
        };

        Response response = Request.Get(getProxyUrl("test?parameterName=parameterValue"))
                .version(HttpVersion.HTTP_1_0)
                .connectTimeout(1000).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is("HTTP/1.0"));
    }

    @Test
    public void proxyProtocolHttp1_1() throws Exception
    {
        handlerExtender = new EchoRequestHandlerExtender()
        {
            @Override
            protected String selectRequestPartToReturn(org.eclipse.jetty.server.Request baseRequest)
            {
                return baseRequest.getProtocol();
            }
        };
        assertRequestOk(getProxyUrl("test?parameterName=parameterValue"), "HTTP/1.1");
    }

    @Ignore // TODO: MULE-8038 - See why listener is still receiving requests after this point
    @Test
    public void proxyStreaming() throws Exception
    {
        final Latch latch = new Latch();
        consumeAllRequest = false;
        handlerExtender = new RequestHandlerExtender()
        {
            AtomicBoolean handled = new AtomicBoolean(false);
            @Override
            public void handleRequest(org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                if( !handled.getAndSet(true) )
                {
                    extractHeadersFromBaseRequest(baseRequest);

                    latch.release();
                    IOUtils.toString(baseRequest.getInputStream());

                    response.setContentType(request.getContentType());
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().print("OK");
                }
            }
        };

        AsyncHttpClientConfig.Builder configBuilder = new AsyncHttpClientConfig.Builder();
        AsyncHttpClientConfig config = configBuilder.build();
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config);
        RequestBuilder requestBuilder = new RequestBuilder();
        requestBuilder.setMethod("POST");
        requestBuilder.setUrl(getProxyUrl("test?parameterName=parameterValue"));
        requestBuilder.setBody(new InputStreamBodyGenerator(new TestInputStream(latch)));
        ListenableFuture<com.ning.http.client.Response> future = asyncHttpClient.executeRequest(requestBuilder.build());
        com.ning.http.client.Response response = future.get();
        assertThat(response.getStatusCode(), is(200));
        response.getHeaders();

        assertThat(getFirstReceivedHeader(HttpHeaders.Names.TRANSFER_ENCODING), is(HttpHeaders.Values.CHUNKED));
        assertThat(response.getResponseBody(), is("OK"));

        asyncHttpClient.close();
    }

    @Test
    public void proxyPath() throws Exception
    {
        handlerExtender = new EchoRequestHandlerExtender()
        {
            @Override
            protected String selectRequestPartToReturn(org.eclipse.jetty.server.Request baseRequest)
            {
                return baseRequest.getPathInfo();
            }
        };
        assertRequestOk(getProxyUrl("test?parameterName=parameterValue"), "/test");
    }

    @Test
    public void proxyQueryString() throws Exception
    {
        handlerExtender = new EchoRequestHandlerExtender()
        {
            @Override
            protected String selectRequestPartToReturn(org.eclipse.jetty.server.Request baseRequest)
            {
                return baseRequest.getQueryString();
            }
        };
        assertRequestOk(getProxyUrl("test?parameterName=parameterValue"), "parameterName=parameterValue");
    }

    @Test
    public void proxyBody() throws Exception
    {
        handlerExtender = new EchoRequestHandlerExtender()
        {
            @Override
            protected String selectRequestPartToReturn(org.eclipse.jetty.server.Request baseRequest)
            {
                return body;
            }
        };

        Response response = Request.Post(getProxyUrl("test"))
            .bodyString("Some Text", ContentType.DEFAULT_TEXT)
            .connectTimeout(1000).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is("Some Text"));
    }

    @Test
    public void proxyHeaders() throws Exception
    {
        handlerExtender = null;

        Response response = Request.Get(getProxyUrl("/test?name=value"))
                .addHeader("MyCustomHeaderName", "MyCustomHeaderValue")
                .connectTimeout(1000).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));

        assertThat(httpResponse.getFirstHeader("MyCustomHeaderName").getValue(), is("MyCustomHeaderValue"));

        Set<String> lowerCaseHeaderNames = new HashSet<>();
        for(Header header : httpResponse.getAllHeaders())
        {
            lowerCaseHeaderNames.add(header.getName().toLowerCase());
            // Ensure no synthetic properties in headers
            assertThat(header.getName(), not(startsWith("http.")));
        }

        // Ensure not repeated headers
        assertThat(lowerCaseHeaderNames.size(), is(httpResponse.getAllHeaders().length));
    }


    private void assertRequestOk(String url, String expectedResponse) throws IOException
    {
        Response response = Request.Get(url).connectTimeout(1000).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        if(expectedResponse!=null)
        {
            assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(expectedResponse));
        }
    }

    private String getProxyUrl(String path)
    {
        return String.format("http://localhost:%s/%s", proxyPort.getNumber(), path);
    }

    private String getServerUrl(String path)
    {
        return String.format("http://localhost:%s/%s", httpPort.getNumber(), path);
    }

    protected void handleRequest(org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(consumeAllRequest)
        {
            extractBaseRequestParts(baseRequest);
        }

        if( handlerExtender==null )
        {
            writeResponse(response);
        }
        else
        {
            handlerExtender.handleRequest(baseRequest, request, response);
        }
    }

    private static interface RequestHandlerExtender
    {
        void handleRequest(org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException;
    }

    private static abstract class EchoRequestHandlerExtender implements RequestHandlerExtender
    {
        @Override
        public void handleRequest(org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
        {
            response.setContentType(request.getContentType());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(selectRequestPartToReturn(baseRequest));
        }

        protected abstract String selectRequestPartToReturn(org.eclipse.jetty.server.Request baseRequest);
    }

}
