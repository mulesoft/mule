/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerParseRequestTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");
  @Rule
  public DynamicPort listenPort2 = new DynamicPort("port2");

  @Override
  protected String getConfigFile() {
    return "http-listener-parse-request-config.xml";
  }

  @Test
  public void parseRequestListenerAttributeWithConfigWithoutParseRequestAttribute() throws Exception {
    sendUrlEncodedPost("listenerWithConfigWithParseRequestNoValue", listenPort.getNumber());
    assertMessageContains(InputStream.class);
  }

  @Test
  public void parseRequestListenerConfigWithParseRequestAttribute() throws Exception {
    sendUrlEncodedPost("listenerWithConfigWithParseRequestValue", listenPort2.getNumber());
    assertMessageContains(InputStream.class);
  }

  @Test
  public void parseRequestListenerOverridesListenerConfigParseRequestAttribute() throws Exception {
    sendUrlEncodedPost("parseRequestUsingListenerValue", listenPort2.getNumber());
    assertMessageContains(Map.class);
  }

  private void sendUrlEncodedPost(String path, int port) throws IOException {
    Request.Post(getUrl(path, port)).bodyForm(new BasicNameValuePair("key", "value")).connectTimeout(RECEIVE_TIMEOUT)
        .socketTimeout(RECEIVE_TIMEOUT).execute();
  }

  private void assertMessageContains(Class type) throws Exception {
    final MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertThat(message.getPayload(), instanceOf(type));
  }

  private String getUrl(String path, int port) {
    return String.format("http://localhost:%s/%s", port, path);
  }

}
