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

import org.apache.http.HttpResponse;
import org.junit.ClassRule;
import org.junit.Test;


public class HttpTlsContextInsecureModeTestCase extends AbstractHttpTlsContextTestCase {

  @ClassRule
  public static final DynamicPort httpsPort = new DynamicPort("httpsPort");
  @ClassRule
  public static final DynamicPort httpsInternalInsecurePort = new DynamicPort("https.internal.insecure");
  @ClassRule
  public static final DynamicPort httpsInternalSecurePort = new DynamicPort("https.internal.secure");
  @ClassRule
  public static final DynamicPort httpsInternalSecureInvalidPort = new DynamicPort("https.internal.secure.invalid");
  @ClassRule
  public static final DynamicPort httpsInternalDefaultPort = new DynamicPort("https.internal.default");
  @ClassRule
  public static final DynamicPort httpsInternalDefaultInvalidPort = new DynamicPort("https.internal.default.invalid");

  private static final String urlPrefix = "https://localhost:" + httpsPort.getValue();
  private static final String insecureModeUrl = urlPrefix + "/test/insecure";
  private static final String secureModeUrl = urlPrefix + "/test/securePass";
  private static final String secureModeInvalidUrl = urlPrefix + "/test/secureFails";
  private static final String defaultModeUrl = urlPrefix + "/test/defaultPass";
  private static final String defaultModeInvalidUrl = urlPrefix + "/test/defaultFails";
  private static final String OK_RESPONSE = "ok";
  private static final String ERROR_RESPONSE = "General SSLEngine problem";

  @Override
  protected String getConfigFile() {
    return "http-tls-insecure-config.xml";
  }

  @Test
  public void testGlobalTlsContextInsecureModeListener() throws Exception {
    HttpResponse response = executeGetRequest(insecureModeUrl);

    assertThat(response, hasStatusCode(SC_OK));
    assertThat(response, body(is(OK_RESPONSE)));
  }

  @Test
  public void testGlobalTlsContextSecureModeListener() throws Exception {
    HttpResponse response = executeGetRequest(secureModeUrl);

    assertThat(response, hasStatusCode(SC_OK));
    assertThat(response, body(is(OK_RESPONSE)));
  }

  @Test
  public void testGlobalTlsContextSecureModeInvalidListener() throws Exception {
    HttpResponse response = executeGetRequest(secureModeInvalidUrl);

    assertThat(response, hasStatusCode(SC_INTERNAL_SERVER_ERROR));
    assertThat(response, body(is(ERROR_RESPONSE)));
  }

  @Test
  public void testGlobalTlsContextDefaultModeListener() throws Exception {
    HttpResponse response = executeGetRequest(defaultModeUrl);

    assertThat(response, hasStatusCode(SC_OK));
    assertThat(response, body(is(OK_RESPONSE)));
  }

  @Test
  public void testGlobalTlsContextDefaultModeInvalidListener() throws Exception {
    HttpResponse response = executeGetRequest(defaultModeInvalidUrl);

    assertThat(response, hasStatusCode(SC_INTERNAL_SERVER_ERROR));
    assertThat(response, body(is(ERROR_RESPONSE)));
  }

}
