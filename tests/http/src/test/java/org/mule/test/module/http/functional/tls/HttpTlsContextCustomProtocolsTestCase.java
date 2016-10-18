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
import static org.mule.test.module.http.functional.matcher.HttpResponseContentStringMatcher.body;
import static org.mule.test.module.http.functional.matcher.HttpResponseStatusCodeMatcher.hasStatusCode;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.apache.http.HttpResponse;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;


public class HttpTlsContextCustomProtocolsTestCase extends AbstractHttpTlsContextTestCase {

  @ClassRule
  public static final DynamicPort httpsPort = new DynamicPort("httpsPort");
  @ClassRule
  public static final DynamicPort httpsInternalDefaultPort = new DynamicPort("https.internal.default");
  @ClassRule
  public static final DynamicPort httpsInternalTls11Port = new DynamicPort("https.internal.tlsv11");
  @ClassRule
  public static final DynamicPort httpsInternalTls12Port = new DynamicPort("https.internal.tlsv12");
  @ClassRule
  public static final DynamicPort httpsInternalAllTlsPort = new DynamicPort("https.internal.alltls");
  @ClassRule
  public static SystemProperty enabledProtocols = new SystemProperty("enabledProtocols", "TLSv1.2");
  @ClassRule
  public static SystemProperty verboseExceptions = new SystemProperty("mule.verbose.exceptions", "true");

  private static final String urlPrefix = "https://localhost:" + httpsPort.getValue();
  private static final String defaultProtocolsPassUrl = urlPrefix + "/test/defaultPass";
  private static final String defaultProtocolsFailsUrl = urlPrefix + "/test/defaultFails";
  private static final String customInvalidProtocolsUrl = urlPrefix + "/test/customInvalid";
  private static final String customValidProtocolsUrl = urlPrefix + "/test/customValid";
  private static final String customProtocolsPropertyUrl = urlPrefix + "/test/property";
  private static final String OK_RESPONSE = "ok";
  private static final String ERROR_RESPONSE = "Error sending HTTP request.";

  @Override
  protected String getConfigFile() {
    return "http-tls-protocols-config.xml";
  }

  @Test
  public void testGlobalTlsContextDefaultProtocolsPass() throws Exception {
    HttpResponse response = executeGetRequest(defaultProtocolsPassUrl);

    assertThat(response, hasStatusCode(SC_OK));
    assertThat(response, body(is(OK_RESPONSE)));
  }

  @Test
  public void testGlobalTlsContextDefaultProtocolsFails() throws Exception {
    HttpResponse response = executeGetRequest(defaultProtocolsFailsUrl);

    assertThat(response, hasStatusCode(SC_OK));
    assertThat(response, body(is(OK_RESPONSE)));
  }

  @Test
  @Ignore("MULE-10752")
  public void testGlobalTlsContextCustomProtocolsRestrictive() throws Exception {
    HttpResponse response = executeGetRequest(customInvalidProtocolsUrl);

    assertThat(response, hasStatusCode(SC_INTERNAL_SERVER_ERROR));
    assertThat(response, body(is(ERROR_RESPONSE)));
  }

  @Test
  public void testGlobalTlsContextCustomProtocolsWider() throws Exception {
    HttpResponse response = executeGetRequest(customValidProtocolsUrl);

    assertThat(response, hasStatusCode(SC_OK));
    assertThat(response, body(is(OK_RESPONSE)));
  }

  @Test
  public void testGlobalTlsContextCustomProtocolsProperty() throws Exception {
    HttpResponse response = executeGetRequest(customProtocolsPropertyUrl);

    assertThat(response, hasStatusCode(SC_OK));
    assertThat(response, body(is(OK_RESPONSE)));
  }

}
