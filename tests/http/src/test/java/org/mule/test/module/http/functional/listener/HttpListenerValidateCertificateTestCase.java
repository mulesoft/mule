/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.mule.functional.functional.FlowAssert.verify;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpListenerValidateCertificateTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort portWithValidation = new DynamicPort("port1");

  @Rule
  public DynamicPort portWithoutValidation = new DynamicPort("port2");

  // Uses a new HttpClient because it is needed to configure the TLS context per test
  public HttpClient httpClientWithCertificate;

  private DefaultTlsContextFactory tlsContextFactory;

  @Override
  protected String getConfigFile() {
    return "http-listener-validate-certificate-config.xml";
  }

  @Before
  public void setup() throws RegistrationException, IOException, InitialisationException {
    tlsContextFactory = new DefaultTlsContextFactory();

    // Configure trust store in the client with the certificate of the server.
    tlsContextFactory.setTrustStorePath("tls/trustStore");
    tlsContextFactory.setTrustStorePassword("mulepassword");
  }

  @After
  public void after() {
    if (httpClientWithCertificate != null) {
      httpClientWithCertificate.stop();
    }
  }

  @Test(expected = IOException.class)
  public void serverWithValidationRejectsRequestWithInvalidCertificate() throws Exception {
    initialiseIfNeeded(tlsContextFactory);
    createHttpClient();

    // Send a request without configuring key store in the client.
    sendRequest(getUrl(portWithValidation.getNumber()), TEST_MESSAGE);
  }

  @Test
  public void serverWithValidationAcceptsRequestWithValidCertificate() throws Exception {
    configureClientKeyStore();
    initialiseIfNeeded(tlsContextFactory);
    createHttpClient();

    assertValidRequest(getUrl(portWithValidation.getNumber()));
    verify("listenerWithTrustStoreFlow");
  }

  @Test
  public void serverWithoutValidationAcceptsRequestWithInvalidCertificate() throws Exception {
    initialiseIfNeeded(tlsContextFactory);
    createHttpClient();

    // Send a request without configuring key store in the client.
    assertValidRequest(getUrl(portWithoutValidation.getNumber()));
  }

  @Test
  public void serverWithoutValidationAcceptsRequestWithValidCertificate() throws Exception {
    configureClientKeyStore();
    initialiseIfNeeded(tlsContextFactory);
    createHttpClient();

    assertValidRequest(getUrl(portWithoutValidation.getNumber()));
  }

  public void createHttpClient() {
    httpClientWithCertificate = getService(HttpService.class).getClientFactory()
        .create(new HttpClientConfiguration.Builder().setTlsContextFactory(tlsContextFactory).build());
    httpClientWithCertificate.start();
  }

  private String sendRequest(String url, String payload) throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri(url).setMethod(POST).setEntity(new ByteArrayHttpEntity(payload.getBytes())).build();
    final HttpResponse response = httpClientWithCertificate.send(request, RECEIVE_TIMEOUT, false, null);

    return IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
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
