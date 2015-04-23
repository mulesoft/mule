/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.http;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.NOT_FOUND;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.util.ClassUtils.getClassPathRoot;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.ssl.DefaultTlsContextFactory;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerStaticResourcesTestCase extends FunctionalTestCase
{

    public static final String INDEX_HTML_CONTENT = "Test index.html";
    public static final String MAIN_HTML_CONTENT = "Test main.html";
    public static final String TESTING_ROOT_FOLDER_SYSTEM_PROPERTY = "test.root";

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");
    @Rule
    public DynamicPort port2 = new DynamicPort("port2");
    @Rule
    public DynamicPort port3 = new DynamicPort("port3");
    @Rule
    public SystemProperty testRoot = new SystemProperty(TESTING_ROOT_FOLDER_SYSTEM_PROPERTY, getClassPathRoot(HttpListenerStaticResourcesTestCase.class).getPath());

    private int responseCode;
    private String payload;
    private MuleMessage response;
    private DefaultTlsContextFactory tlsContextFactory;

    @Override
    protected String getConfigFile()
    {
        return "http-listener-static-resource-test.xml";
    }

    @Before
    public void setup() throws IOException
    {
        tlsContextFactory = new DefaultTlsContextFactory();

        // Configure trust store in the client with the certificate of the server.
        tlsContextFactory.setTrustStorePath("trustStore");
        tlsContextFactory.setTrustStorePassword("mulepassword");
    }

    @Test
    public void httpUrlWithoutExplicitResourceShouldReturnDefaultDocument() throws Exception
    {
        String url = format("http://localhost:%d/static", port1.getNumber());
        executeRequest(url, true);
        assertThat(OK.getStatusCode(), is(responseCode));
        assertThat(payload, is(INDEX_HTML_CONTENT));
    }

    @Test
    public void httpUrlRequestingExplicitResourceShouldReturnResource() throws Exception
    {
        String url = format("http://localhost:%d/static/main.html", port1.getNumber());
        executeRequest(url, true);
        assertThat(OK.getStatusCode(), is(responseCode));
        assertThat(payload, is(MAIN_HTML_CONTENT));
    }

    @Test
    public void httpUrlRequestingNonexistentResourceShouldReturnNotFoundStatus() throws Exception
    {
        String url = format("http://localhost:%d/static/foo.html", port1.getNumber());
        executeRequest(url, true);
        assertThat(NOT_FOUND.getStatusCode(), is(responseCode));
    }

    @Test
    public void contentTypeForDefaultResourceShouldBeTextHtml() throws Exception
    {
        String url = format("http://localhost:%d/static", port1.getNumber());
        executeRequest(url, true);
        assertThat(OK.getStatusCode(), is(responseCode));
        assertResponseContentType("text/html");
    }

    @Test
    public void contentTypeShouldBeDetermintedFromResource() throws Exception
    {
        String url = format("http://localhost:%d/static/image.gif", port1.getNumber());
        executeRequest(url, true);
        assertThat(OK.getStatusCode(), is(responseCode));
        assertResponseContentType("image/gif");
    }

    @Test
    public void explicitMimeTypeConfigurationShouldOverrideDefaults() throws Exception
    {
        String url = format("http://localhost:%d/static/image.png", port1.getNumber());
        executeRequest(url, true);
        assertThat(OK.getStatusCode(), is(responseCode));
        assertResponseContentType("image/png");
    }

    @Test
    public void httpsUrlWithoutExplicitResourceShouldReturnDefaultDocument() throws Exception
    {
        String url = format("https://localhost:%d/static", port2.getNumber());
        executeRequest(url, true);
        assertThat(OK.getStatusCode(), is(responseCode));
        assertThat(payload, is(INDEX_HTML_CONTENT));
    }

    @Test
    public void httpsUrlRequestingExplicitResourceShouldReturnResource() throws Exception
    {
        String url = format("https://localhost:%d/static/main.html", port2.getNumber());
        executeRequest(url, true);
        assertThat(OK.getStatusCode(), is(responseCode));
        assertThat(payload, is(MAIN_HTML_CONTENT));
    }

    @Test
    public void httpsUrlRequestingNonexistentResourceShouldReturnNotFoundStatus() throws Exception
    {
        String url = format("https://localhost:%d/static/foo.html", port2.getNumber());
        executeRequest(url, true);
        assertThat(NOT_FOUND.getStatusCode(), is(responseCode));
    }

    private void assertResponseContentType(String contentType)
    {
        assertThat(contentType, is(response.getInboundProperty(CONTENT_TYPE.toLowerCase())));
    }

    /**
     * Test that endpoints bound to the same http port but different path work with
     * the static resource MP
     */
    @Test
    public void testFlowBindingOnSamePort() throws Exception
    {
        String url = format("http://localhost:%d/echo", port1.getNumber());
        executeRequest(url, true);
        assertThat(OK.getStatusCode(), is(responseCode));

        url = format("https://localhost:%d/echo", port2.getNumber());
        executeRequest(url, true);
        assertThat(OK.getStatusCode(), is(responseCode));
    }

    @Test
    public void httpUrlWithRootAddressShouldReturnDefaultDocument() throws Exception
    {
        String url = format("http://localhost:%d/", port3.getNumber());
        executeRequest(url, false);
        assertThat(OK.getStatusCode(), is(responseCode));
        assertThat(response.getPayloadAsString(), is(INDEX_HTML_CONTENT));
    }

    @Test
    public void httpUrlExplicitResourceInRootPathShouldReturnResource() throws Exception
    {
        String url = format("http://localhost:%d/index.html", port3.getNumber());
        executeRequest(url, false);
        assertThat(OK.getStatusCode(), is(responseCode));
        assertThat(payload, is(INDEX_HTML_CONTENT));
    }

    private void executeRequest(String url, boolean followRedirects) throws Exception
    {
        HttpRequestOptionsBuilder optionsBuilder = HttpRequestOptionsBuilder.newOptions().disableStatusCodeValidation();
        if (!followRedirects)
        {
            optionsBuilder.disableFollowsRedirect();
        }
        if (url.startsWith(HTTPS.getScheme()))
        {
            optionsBuilder.tlsContextFactory(tlsContextFactory);
        }
        response = muleContext.getClient().send(url, getTestMuleMessage(), optionsBuilder.build());
        responseCode = response.getInboundProperty(HTTP_STATUS_PROPERTY);
        payload = response.getPayloadAsString();
    }

}
