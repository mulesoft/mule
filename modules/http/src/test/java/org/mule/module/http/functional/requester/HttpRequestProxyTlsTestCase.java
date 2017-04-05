/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpConstants.RequestProperties.HTTP_REQUEST_URI;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.construct.Flow;
import org.mule.module.http.functional.TestProxyServer;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


@RunWith(Parameterized.class)
public class HttpRequestProxyTlsTestCase extends FunctionalTestCase
{

    private static final String PROXY_RESPONSE = "HTTP/1.1 200 Connection established\r\n\r\n";
    private static final String OK_RESPONSE = "OK";
    private static final String PATH = "/test?key=value";

    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Rule
    public SystemProperty keyStorePathProperty;

    @Rule
    public SystemProperty trustStorePathProperty;

    private TestProxyServer proxyServer = new TestProxyServer(proxyPort.getNumber(), httpPort.getNumber());

    private String requestURI;
    private String requestPayload;
    private String requestHost;

    public HttpRequestProxyTlsTestCase(String keyStorePath, String trustStorePath, String requestHost)
    {
        this.keyStorePathProperty = new SystemProperty("keyStorePath", keyStorePath);
        this.trustStorePathProperty = new SystemProperty("trustStorePath", trustStorePath);
        this.requestHost = requestHost;
    }

    /**
     * The test will run with two key store / trust store pairs. One has the subject alternative name set to localhost (the
     * default for all TLS tests), and the other one has the name set to "test". We need this to validate that the hostname
     * verification is performed using the host of the request, and not the one of the proxy.
     */
    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {{"ssltest-keystore-with-test-hostname.jks", "ssltest-truststore-with-test-hostname.jks", "test"},
                {"ssltest-keystore.jks", "ssltest-cacerts.jks", "localhost"}});
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-proxy-tls-config.xml";
    }

    @Test
    public void requestIsSentCorrectlyThroughHttpsProxy() throws Exception
    {
        getFunctionalTestComponent("serverFlow").setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                requestPayload = context.getMessage().getPayloadAsString();
                requestURI = context.getMessage().getInboundProperty(HTTP_REQUEST_URI);
            }
        });

        proxyServer.start();

        Flow flow = (Flow) getFlowConstruct("clientFlow");
        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event.setFlowVariable("host", requestHost);
        event.setFlowVariable("path", PATH);
        event = flow.process(event);

        assertThat(requestPayload, equalTo(TEST_MESSAGE));
        assertThat(requestURI, equalTo(PATH));
        assertThat(event.getMessage().<Integer>getInboundProperty(HTTP_STATUS_PROPERTY), equalTo(OK.getStatusCode()));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(OK_RESPONSE));

        proxyServer.stop();
    }
}
