/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.spring.security;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_USER_PROPERTY;
import static org.mule.runtime.core.api.security.DefaultMuleCredentials.createHeader;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.service.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.service.http.api.HttpConstants.Method.GET;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("See MULE-9202")
public class PlainTextFunctionalTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  /**
   * This client is used to hit http listeners under test.
   */
  protected HttpClient httpClient;

  @Before
  public void createHttpClient() throws RegistrationException, IOException, InitialisationException {
    httpClient = muleContext.getRegistry().lookupObject(HttpService.class).getClientFactory()
        .create(new HttpClientConfiguration.Builder().build());
    httpClient.start();
  }

  @After
  public void disposeHttpClient() {
    httpClient.stop();
  }

  @Override
  protected String getConfigFile() {
    // Note that this file contains global attributes, which the configuration-building
    // process will ignore (MULE-5375)
    return "encryption-test-flow.xml";
  }

  @Test
  public void testAuthenticationFailureNoContext() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri(getUrl()).setMethod(GET).setEntity(new ByteArrayHttpEntity(TEST_PAYLOAD.getBytes())).build();
    final HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    assertNotNull(response);
    assertThat(response.getStatusCode(), is(UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void testAuthenticationFailureBadCredentials() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri(getUrl()).setMethod(GET).setEntity(new ByteArrayHttpEntity(TEST_PAYLOAD.getBytes()))
            .addHeader(MULE_USER_PROPERTY, createHeader("anonX", "anonX".toCharArray())).build();
    final HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    assertNotNull(response);
    assertThat(response.getStatusCode(), is(UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void testAuthenticationAuthorised() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri(getUrl()).setMethod(GET).setEntity(new ByteArrayHttpEntity(TEST_PAYLOAD.getBytes()))
            .addHeader(MULE_USER_PROPERTY, createHeader("anon", "anon".toCharArray())).build();
    final HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    assertNotNull(response);
    assertThat(response.getStatusCode(), is(OK.getStatusCode()));
  }

  private String getUrl() {
    return format("http://localhost:%s/index.html", port1.getNumber());
  }
}
