/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.service.http.api.HttpConstants.Method.GET;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class InOptionalOutTestCase extends AbstractIntegrationTestCase {

  public static final long TIMEOUT = 3000;

  @Rule
  public DynamicPort port = new DynamicPort("port1");

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
    return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out-flow.xml";
  }

  @Test
  public void testExchange() throws Exception {
    String listenerUrl = format("http://localhost:%s/", port.getNumber());
    HttpRequest request = HttpRequest.builder().setUri(listenerUrl).setMethod(GET)
        .setEntity(new ByteArrayHttpEntity("some data".getBytes())).build();
    HttpEntity responseEntity = httpClient.send(request, RECEIVE_TIMEOUT, false, null).getEntity();

    assertNotNull(responseEntity);
    assertEquals(StringUtils.EMPTY, IOUtils.toString(((InputStreamHttpEntity) responseEntity).getInputStream()));

    request = HttpRequest.builder().setUri(listenerUrl).setMethod(GET).addHeader("foo", "bar")
        .setEntity(new ByteArrayHttpEntity("some data".getBytes())).build();
    responseEntity = httpClient.send(request, RECEIVE_TIMEOUT, false, null).getEntity();
    assertNotNull(responseEntity);
    assertEquals("foo header received", IOUtils.toString(((InputStreamHttpEntity) responseEntity).getInputStream()));
  }
}
