/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.service.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.service.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.module.http.functional.matcher.HttpResponseReasonPhraseMatcher.hasReasonPhrase;
import static org.mule.test.module.http.functional.matcher.HttpResponseStatusCodeMatcher.hasStatusCode;
import static java.lang.String.format;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

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
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpListenerAuthenticationTestCase extends AbstractHttpTestCase {

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
  protected String getConfigFile() {
    return "http-listener-authentication-config.xml";
  }

  @After
  public void tearDown() {
    IOUtils.closeQuietly(httpResponse);
    IOUtils.closeQuietly(httpClient);
  }

  @Test
  public void invalidBasicAuthentication() throws Exception {
    CredentialsProvider credsProvider = getCredentialsProvider(VALID_USER, INVALID_PASSWORD);
    getHttpResponse(credsProvider);

    assertThat(httpResponse.getStatusLine().getStatusCode(), is(UNAUTHORIZED.getStatusCode()));
    Header authHeader = httpResponse.getFirstHeader(WWW_AUTHENTICATE);
    assertThat(authHeader, is(notNullValue()));
    assertThat(authHeader.getValue(), is(BASIC_REALM_MULE_REALM));
    assertThat(muleContext.getClient().request("test://unauthorized", RECEIVE_TIMEOUT).getRight().isPresent(), is(true));
  }

  @Test
  public void validBasicAuthentication() throws Exception {
    CredentialsProvider credsProvider = getCredentialsProvider(VALID_USER, VALID_PASSWORD);
    getHttpResponse(credsProvider);

    assertThat(httpResponse.getStatusLine().getStatusCode(), is(OK.getStatusCode()));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(EXPECTED_PAYLOAD));
  }

  @Test
  public void noProvider() throws Exception {
    CredentialsProvider credsProvider = getCredentialsProvider(VALID_USER, VALID_PASSWORD);
    getHttpResponse(credsProvider, "zaraza");

    assertThat(httpResponse, hasStatusCode(INTERNAL_SERVER_ERROR.getStatusCode()));
    assertThat(httpResponse, hasReasonPhrase(INTERNAL_SERVER_ERROR.getReasonPhrase()));
    assertThat(muleContext.getClient().request("test://security", RECEIVE_TIMEOUT).getRight().isPresent(), is(true));
  }

  private void getHttpResponse(CredentialsProvider credsProvider) throws IOException {
    getHttpResponse(credsProvider, "memory-provider");
  }

  private void getHttpResponse(CredentialsProvider credsProvider, String provider) throws IOException {
    HttpPost httpPost = new HttpPost(format("http://localhost:%s/basic?provider=%s", listenPort.getNumber(), provider));
    httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
    httpResponse = httpClient.execute(httpPost);
  }

  private CredentialsProvider getCredentialsProvider(String user, String password) {
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
    return credsProvider;
  }

}
