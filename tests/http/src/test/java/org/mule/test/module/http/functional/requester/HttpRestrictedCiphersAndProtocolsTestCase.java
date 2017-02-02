/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.functional.junit4.rules.ExpectedError;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Sets up some HTTPS servers and clients with different protocols and ciphers. Verifies only matching configurations are
 * successful interacting with each other.
 */
public class HttpRestrictedCiphersAndProtocolsTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");
  @Rule
  public DynamicPort port2 = new DynamicPort("port2");
  @Rule
  public DynamicPort port3 = new DynamicPort("port3");
  @Rule
  public SystemProperty cipherSuites = new SystemProperty("cipherSuites", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA");
  @Rule
  public SystemProperty protocol = new SystemProperty("protocol", "HTTPS");
  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  private HttpRequestOptionsBuilder optionsBuilder = HttpRequestOptionsBuilder.newOptions().method(POST.name());
  private DefaultTlsContextFactory tlsContextFactory;

  @Override
  protected String getConfigFile() {
    return "http-restricted-ciphers-and-protocols-config.xml";
  }

  @Before
  public void setUp() throws IOException {
    tlsContextFactory = new DefaultTlsContextFactory();
    tlsContextFactory.setTrustStorePath("tls/trustStore");
    tlsContextFactory.setTrustStorePassword("mulepassword");
  }

  @Test
  public void worksWithProtocolAndCipherSuiteMatch() throws Exception {
    Event response = flowRunner("12Client12Server").withPayload(TEST_PAYLOAD).run();
    assertThat(response.getMessageAsString(muleContext), is(TEST_PAYLOAD));
  }

  @Test
  public void worksWithProtocolMatch() throws Exception {
    // Uses default ciphers and protocols
    HttpRequestOptions requestOptions = optionsBuilder.tlsContextFactory(tlsContextFactory).build();
    InternalMessage response = muleContext.getClient().send(String.format("https://localhost:%s", port1.getValue()),
                                                            InternalMessage.of(TEST_PAYLOAD), requestOptions)
        .getRight();
    assertThat(muleContext.getTransformationService().transform(response, DataType.STRING).getPayload().getValue(),
               is(TEST_PAYLOAD));
  }

  @Test
  public void worksWithCipherSuiteMatch() throws Exception {
    // Forces TLS_DHE_DSS_WITH_AES_128_CBC_SHA
    tlsContextFactory.setEnabledCipherSuites(cipherSuites.getValue());
    HttpRequestOptions requestOptions = optionsBuilder.tlsContextFactory(tlsContextFactory).build();
    InternalMessage response = muleContext.getClient().send(String.format("https://localhost:%s", port3.getValue()),
                                                            InternalMessage.of(TEST_PAYLOAD), requestOptions)
        .getRight();
    assertThat(muleContext.getTransformationService().transform(response, DataType.STRING).getPayload().getValue(),
               is(TEST_PAYLOAD));
  }

  @Test
  public void failsWithProtocolMismatch() throws Exception {
    expectedError.expectCause(instanceOf(IOException.class));
    flowRunner("12Client1Server").withPayload(TEST_PAYLOAD).run();
  }

  @Test
  public void failsWithCipherSuiteMismatch() throws Exception {
    expectedError.expectCause(instanceOf(IOException.class));
    flowRunner("12CipherClient1CipherServer").withPayload(TEST_PAYLOAD).run();
  }
}
