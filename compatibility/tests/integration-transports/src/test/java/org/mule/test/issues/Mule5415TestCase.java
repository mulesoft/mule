/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.issues;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;


public class Mule5415TestCase extends CompatibilityFunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/issues/mule-5415-config.xml";
  }

  @Test
  public void testFirstRequestDoesNotFail() throws Exception {
    HttpRequest httpRequest = HttpRequest.builder().setUri(format("http://localhost:%s?param1=1&param2=3", port1.getNumber()))
        .addHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED.toRfcString()).build();
    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    assertThat(httpResponse.getStatusCode(), is(OK.getStatusCode()));
  }
}
