/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.tls;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.test.module.http.functional.matcher.HttpResponseContentStringMatcher.contentValue;
import static org.mule.test.module.http.functional.matcher.HttpResponseStatusCodeMatcher.hasStatusCode;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.apache.http.HttpResponse;
import org.junit.ClassRule;
import org.junit.Test;


public class HttpTlsContextCustomCiphersTestCase extends AbstractHttpTlsContextTestCase {

  @ClassRule
  public static DynamicPort httpsPort = new DynamicPort("httpsPort");
  @ClassRule
  public static DynamicPort httpsInternalPort1 = new DynamicPort("internal.port.1");
  @ClassRule
  public static DynamicPort httpsInternalPort2 = new DynamicPort("internal.port.2");
  @ClassRule
  public static DynamicPort httpsInternalPort3 = new DynamicPort("internal.port.3");

  private static final String invalidCipher = "SSL_DH_anon_WITH_DES_CBC_SHA";
  private static final String urlPrefix = "https://localhost:" + httpsPort.getValue();
  private static final String bothProtocolsOneCipher = urlPrefix + "/test/bothProtocolsOneCipher";
  private static final String validProtocolValidCipher = urlPrefix + "/test/validProtocolValidCipher";
  private static final String validProtocolInvalidCipher = urlPrefix + "/test/validProtocolInvalidCipher";
  private static final String OK_RESPONSE = "ok";
  private static final String ERROR_RESPONSE = "Error sending HTTP request.";

  @ClassRule
  public static SystemProperty cipherSuites = new SystemProperty("cipherSuites", invalidCipher);
  @ClassRule
  public static SystemProperty verboseExpcetions = new SystemProperty("mule.verbose.exceptions", "true");

  // @BeforeClass
  // public static void setup() throws Exception
  // {
  // mule.deploy(appsPath + customCiphersApplication);
  //
  // mule.start(
  // "-M-Dmule.agent.enabled=false",
  // "-M-DcipherSuites=" + invalidCipher,
  // "-M-Dinternal.port.1=" + httpsInternalPort1.getValue(),
  // "-M-Dinternal.port.2=" + httpsInternalPort2.getValue(),
  // "-M-Dinternal.port.3=" + httpsInternalPort3.getValue(),
  // "-M-DhttpsPort=" + httpsPort.getValue()
  // );
  //
  // prober.check(isDeployed(mule, customCiphersApplication));
  // }

  @Override
  protected String getConfigFile() {
    return "http-tls-ciphers-config.xml";
  }

  @Test
  public void testBothProtocolsOneCipher() throws Exception {
    HttpResponse response = executeGetRequest(bothProtocolsOneCipher);

    assertThat(response, hasStatusCode(SC_OK));
    assertThat(response, contentValue(is(OK_RESPONSE)));
  }

  @Test
  public void testValidProtocolValidCipher() throws Exception {
    HttpResponse response = executeGetRequest(validProtocolValidCipher);

    assertThat(response, hasStatusCode(SC_OK));
    assertThat(response, contentValue(is(OK_RESPONSE)));
  }

  @Test
  public void testValidProtocolInvalidCipher() throws Exception {
    HttpResponse response = executeGetRequest(validProtocolInvalidCipher);

    assertThat(response, hasStatusCode(SC_INTERNAL_SERVER_ERROR));
    assertThat(response, contentValue(is(ERROR_RESPONSE)));
  }

  // @AfterClass
  // public static void stopMule()
  // {
  // mule.undeployAll();
  // mule.stop();
  // prober.check(isNotRunning(mule));
  // }
}
