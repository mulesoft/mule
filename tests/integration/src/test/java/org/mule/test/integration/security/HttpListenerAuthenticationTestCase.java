/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.security;

import static org.apache.commons.httpclient.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerAuthenticationTestCase extends FunctionalTestCase
{

    private static final String BASIC_REALM_MULE_REALM = "Basic realm=\"mule-realm\"";
    private static final String VALID_USER = "user";
    private static final String VALID_PASSWORD = "password";
    private static final String INVALID_PASSWORD = "invalidPassword";
    private static final String EXPECTED_PAYLOAD = "TestBasicAuthOk";
    CloseableHttpClient httpClient;
    CloseableHttpResponse httpResponse;


    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/security/http-listener-authentication-config.xml";
    }

    @After
    public void tearDown()
    {
        IOUtils.closeQuietly(httpResponse);
        IOUtils.closeQuietly(httpClient);
    }

    @Test
    public void invalidBasicAuthentication() throws Exception
    {
        CredentialsProvider credsProvider = getCredentialsProvider(VALID_USER, INVALID_PASSWORD);
        getHttpResponse(credsProvider);

        assertThat(httpResponse.getStatusLine().getStatusCode(), is(SC_UNAUTHORIZED));
        Header authHeader = httpResponse.getFirstHeader(WWW_AUTHENTICATE);
        assertThat(authHeader, is(notNullValue()));
        assertThat(authHeader.getValue(), is(BASIC_REALM_MULE_REALM));
    }

    @Test
    public void validBasicAuthentication() throws Exception
    {
        CredentialsProvider credsProvider = getCredentialsProvider(VALID_USER, VALID_PASSWORD);
        getHttpResponse(credsProvider);

        assertThat(httpResponse.getStatusLine().getStatusCode(), is(SC_OK));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(EXPECTED_PAYLOAD));
    }

    private void getHttpResponse(CredentialsProvider credsProvider) throws IOException
    {
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%s/basic", listenPort.getNumber()));
        httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
        httpResponse = httpClient.execute(httpPost);
    }

    private CredentialsProvider getCredentialsProvider(String user, String password)
    {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
        return credsProvider;
    }
}
