/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.ssl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import org.mule.extension.socket.SocketExtensionTestCase;
import org.mule.extension.socket.api.SocketAttributes;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;

public class TcpSslTestCase extends SocketExtensionTestCase {

  @Rule
  public DynamicPort sslPort = new DynamicPort("port2");

  @Override
  protected String getConfigFile() {
    return "tcp-ssl-config.xml";
  }

  @Test
  public void sendAndReceiveOverSSLConfiguredGlobally() throws Exception {
    MuleMessage muleMessage = sendStringAndAssertResponse("ssl-send-and-receive-global-tls");
    assertClientAutenticathed((SocketAttributes) muleMessage.getAttributes());
  }

  @Test
  public void sendAndReceiveOverSSL() throws Exception {
    MuleMessage muleMessage = sendStringAndAssertResponse("ssl-send-and-receive");
    // trust-store not configured for listenerTlsContext
    assertClientNotAutenticathed((SocketAttributes) muleMessage.getAttributes());

  }

  @Test
  public void multipleSendAndReceiveOverSSLConfiguredGlobally() throws Exception {
    MuleMessage message;
    for (int i = 0; i < REPETITIONS; i++) {
      message = sendStringAndAssertResponse("ssl-send-and-receive-global-tls");
      assertClientAutenticathed((SocketAttributes) message.getAttributes());

    }
  }

  private MuleMessage sendStringAndAssertResponse(String flowName) throws Exception {
    org.mule.runtime.core.api.MuleMessage muleMessage = flowRunner(flowName).withPayload(TEST_STRING).run().getMessage();

    InputStream inputStream = (InputStream) muleMessage.getPayload();

    String response = IOUtils.toString(inputStream);
    assertThat(RESPONSE_TEST_STRING, is(response));
    return muleMessage;
  }

  private void assertClientAutenticathed(SocketAttributes attributes) {
    assertThat(attributes.getLocalCertificates(), is(notNullValue()));
    assertThat(attributes.getPeerCertificates(), is(notNullValue()));
  }

  private void assertClientNotAutenticathed(SocketAttributes attributes) {
    assertThat(attributes.getLocalCertificates(), is(nullValue()));
    assertThat(attributes.getPeerCertificates(), is(notNullValue()));
  }
}
