/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.glassfish.grizzly.http.server.NetworkListener.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.runners.Parameterized.*;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.httpclient.HttpClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class CxfClientProxyRepliesWithEmptyRequestResponse extends AbstractServiceAndFlowTestCase
{

    public static final int SC_GATEWAY_TIMEOUT = 504;
    public static final int SC_ACCEPTED = 202;
    public static final int SC_INTERNAL_SERVER_ERROR = 500;
    private HttpRequestOptions ignoreStatusCodeValidationOptions = HttpRequestOptionsBuilder.newOptions().method(POST.name()).disableStatusCodeValidation().build();
    private HttpServer server;


    public CxfClientProxyRepliesWithEmptyRequestResponse(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        System.setProperty("wsdl.uri", "ClientAndServiceProxy.wsdl");
    }

    @Rule
    public DynamicPort listenerDynamicPort = new DynamicPort("listenerPort");

    @Rule
    public DynamicPort requesterDynamicPort = new DynamicPort("requesterPort");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {ConfigVariant.FLOW, "cxf-client-and-service-proxy.xml"}
        });
    }

    @After
    public void tearDown() throws Exception
    {
        server.shutdown();
    }

    @Test
    public void testCxfProxyRepliesBackOnEmptyResponse() throws Exception
    {
        startSoapServiceResponding(SC_GATEWAY_TIMEOUT);
        int resultStatusCode = makeSoapRequest();
        assertThat(resultStatusCode, is(SC_INTERNAL_SERVER_ERROR));
    }

    @Test(expected = SocketTimeoutException.class)
    public void testCxfProxyTimeoutsOnAcceptedStatusCodeResponseAndEmptyResponse() throws Exception
    {
        startSoapServiceResponding(SC_ACCEPTED);
        makeSoapRequest();
    }

    private void startSoapServiceResponding(Integer responseStatusCode) throws IOException
    {
        server = new HttpServer();
        final Integer responseSC = responseStatusCode;

        NetworkListener serverListener = new NetworkListener("soapServiceServer", DEFAULT_NETWORK_HOST, requesterDynamicPort.getNumber());
        server.addListener(serverListener);
        server.getServerConfiguration().addHttpHandler(new HttpHandler()
        {
            @Override
            public void service(Request request, Response response) throws Exception
            {
                response.setStatus(responseSC);
            }
        }, "/*");

        server.start();
    }


    private int makeSoapRequest() throws IOException, URISyntaxException
    {
        String soapRequestBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:test=\"http://test.Pablo.name/\">"
                                 + "<soapenv:Header/>"
                                 + "<soapenv:Body>"
                                 + "<test:Hi/>"
                                 + "</soapenv:Body>"
                                 + "</soapenv:Envelope>";

        HttpClient client = new HttpClient();
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setSoTimeout(1000);
        client.setParams(clientParams);

        PostMethod soapRequestPostMethod = new PostMethod("http://localhost:" + listenerDynamicPort.getNumber() + "/");
        StringRequestEntity soapPayload = new StringRequestEntity(soapRequestBody, "application/xml", "UTF-8");
        soapRequestPostMethod.setRequestEntity(soapPayload);

        return client.executeMethod(soapRequestPostMethod);
    }

}
