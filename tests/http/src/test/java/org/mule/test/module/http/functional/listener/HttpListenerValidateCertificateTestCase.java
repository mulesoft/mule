/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.functional.functional.FlowAssert.verify;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerValidateCertificateTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort portWithValidation = new DynamicPort("port1");

  @Rule
  public DynamicPort portWithoutValidation = new DynamicPort("port2");

  private DefaultTlsContextFactory tlsContextFactory;

  @Override
  protected String getConfigFile() {
    return "http-listener-validate-certificate-config.xml";
  }

  @Before
  public void setup() throws IOException {
    tlsContextFactory = new DefaultTlsContextFactory();

    // Configure trust store in the client with the certificate of the server.
    tlsContextFactory.setTrustStorePath("tls/trustStore");
    tlsContextFactory.setTrustStorePassword("mulepassword");

  }

  @Test(expected = MessagingException.class)
  public void serverWithValidationRejectsRequestWithInvalidCertificate() throws Exception {
    // Send a request without configuring key store in the client.
    sendRequest(getUrl(portWithValidation.getNumber()), TEST_MESSAGE);
  }

  @Test
  public void serverWithValidationAcceptsRequestWithValidCertificate() throws Exception {
    configureClientKeyStore();
    assertValidRequest(getUrl(portWithValidation.getNumber()));
    verify("listenerWithTrustStoreFlow");
  }

  @Test
  public void serverWithoutValidationAcceptsRequestWithInvalidCertificate() throws Exception {
    // Send a request without configuring key store in the client.
    assertValidRequest(getUrl(portWithoutValidation.getNumber()));
  }

  @Test
  public void serverWithoutValidationAcceptsRequestWithValidCertificate() throws Exception {
    configureClientKeyStore();
    assertValidRequest(getUrl(portWithoutValidation.getNumber()));
  }

  private String sendRequest(String url, String payload) throws Exception {
    MuleMessage response = muleContext.getClient()
        .send(url, getTestMuleMessage(payload), newOptions().method(POST.name()).tlsContextFactory(tlsContextFactory).build())
        .getRight();
    return getPayloadAsString(response);
  }

  private void assertValidRequest(String url) throws Exception {
    assertThat(sendRequest(url, TEST_MESSAGE), equalTo(TEST_MESSAGE));
  }

  /**
   * Configure key store for the client (the server contains this certificate in its trust store)
   */
  private void configureClientKeyStore() throws IOException {
    tlsContextFactory.setKeyStorePath("tls/ssltest-keystore.jks");
    tlsContextFactory.setKeyStorePassword("changeit");
    tlsContextFactory.setKeyManagerPassword("changeit");
  }

  private String getUrl(int port) {
    return String.format("https://localhost:%d/", port);
  }

}
