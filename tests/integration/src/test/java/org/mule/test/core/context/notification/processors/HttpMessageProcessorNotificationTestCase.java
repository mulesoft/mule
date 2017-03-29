/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.Method.GET;

import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.core.context.notification.Node;
import org.mule.test.core.context.notification.RestrictedNode;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class HttpMessageProcessorNotificationTestCase extends AbstractMessageProcessorNotificationTestCase {

  @Rule
  public DynamicPort proxyPort = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @After
  public void disposeHttpClient() {
    httpClient.stop();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/nonblocking-message-processor-notification-test-flow.xml";
  }

  @Test
  public void doTest() throws Exception {
    HttpRequest request = HttpRequest.builder().setUri("http://localhost:" + proxyPort.getValue() + "/in").setMethod(GET)
        .setEntity(new ByteArrayHttpEntity("test".getBytes())).build();
    assertThat(httpClient.send(request, RECEIVE_TIMEOUT, false, null).getEntity(), not(nullValue()));

    assertNotifications();
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node()

        // logger
        .serial(prePost())

        // <response> start
        .serial(pre())

        // logger
        .serial(prePost())

        // request to echo service
        .serial(pre()).serial(prePost()).serial(post())

        // logger
        .serial(prePost())

        // request to echo service
        .serial(pre()).serial(prePost()).serial(post())

        // <response> end
        .serial(pre()).serial(prePost()).serial(post()).serial(post());
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {}
}
