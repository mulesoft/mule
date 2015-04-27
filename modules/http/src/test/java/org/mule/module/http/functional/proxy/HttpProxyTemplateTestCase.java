/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.proxy;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.X_FORWARDED_FOR;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.functional.TestInputStream;
import org.mule.module.http.functional.requester.AbstractHttpRequestTestCase;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;
import org.mule.util.concurrent.Latch;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpProxyTemplateTestCase extends AbstractHttpRequestTestCase
{

    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    private RequestHandlerExtender handlerExtender;
    private boolean consumeAllRequest = true;
    private String configFile;
    private String requestThreadContains;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{"http-proxy-template-config.xml","worker"}, {"http-proxy-template-selectors-config.xml", "SelectorRunner"}});
    }

    public HttpProxyTemplateTestCase(String configFile, String requestThreadContains)
    {
        this.configFile = configFile;
        this.requestThreadContains = requestThreadContains;
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void proxySimpleRequests() throws Exception
    {
        handlerExtender = null;
        assertRequestOk(getProxyUrl(""), null);
        assertRequestOk(getProxyUrl("test"), null);
    }

    @Test
    public void failIfTargetServiceIsDown() throws Exception
    {
        handlerExtender = null;
        stopServer();
        Response response = Request.Get(getProxyUrl("")).connectTimeout(RECEIVE_TIMEOUT).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(500));
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
                .connectTimeout(RECEIVE_TIMEOUT).execute();
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
                .connectTimeout(RECEIVE_TIMEOUT).execute();
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

    @Test
    public void proxyStreaming() throws Exception
    {
        final Latch latch = new Latch();
        consumeAllRequest = false;
        handlerExtender = new RequestHandlerExtender()
        {
            @Override
            public void handleRequest(org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                extractHeadersFromBaseRequest(baseRequest);

                latch.release();
                IOUtils.toString(baseRequest.getInputStream());

                response.setContentType(request.getContentType());
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print("OK");
            }
        };

        AsyncHttpClientConfig.Builder configBuilder = new AsyncHttpClientConfig.Builder();
        AsyncHttpClientConfig config = configBuilder.build();
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config);

        AsyncHttpClient.BoundRequestBuilder boundRequestBuilder = asyncHttpClient.preparePost(getProxyUrl("test?parameterName=parameterValue"));
        boundRequestBuilder.setBody(new InputStreamBodyGenerator(new TestInputStream(latch)));
        ListenableFuture<com.ning.http.client.Response> future = boundRequestBuilder.execute();

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
            .connectTimeout(RECEIVE_TIMEOUT).execute();
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
                .connectTimeout(RECEIVE_TIMEOUT).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));

        assertThat(getFirstReceivedHeader("MyCustomHeaderName"), is("MyCustomHeaderValue"));

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

    @Test
    public void setXForwardedForHeader() throws Exception
    {
        handlerExtender = null;

        Response response = Request.Get(getProxyUrl(""))
                .connectTimeout(RECEIVE_TIMEOUT).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));

        assertThat(getFirstReceivedHeader(X_FORWARDED_FOR), startsWith("/127.0.0.1:"));
    }

    @Test
    public void requestThread() throws Exception
    {
        Request.Get(getProxyUrl("")).connectTimeout(RECEIVE_TIMEOUT).execute();
        SensingNullMessageProcessor sensingMessageProcessor = muleContext.getRegistry().lookupObject("sensingMessageProcessor");
        assertThat(sensingMessageProcessor.thread.getName(), containsString(requestThreadContains));
    }

    private void assertRequestOk(String url, String expectedResponse) throws IOException
    {
        Response response = Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
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
