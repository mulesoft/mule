/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class WireTapCxfTestCase extends CompatibilityFunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/wire-tap-cxf-flow.xml";
  }

  @Test
  public void testWireTap() throws Exception {
    String url = "http://localhost:" + port1.getNumber() + "/services/EchoUMO";
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><echo><text>foo</text></echo></soap:Body></soap:Envelope>";

    HttpRequest httpRequest =
        HttpRequest.builder().setUri(url).setEntity(new ByteArrayHttpEntity(msg.getBytes())).setMethod(POST).build();
    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    String responseString = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertThat(responseString, containsString("echoResponse"));
    assertThat(responseString, not(containsString("soap:Fault")));
    assertThat(muleContext.getClient().request("test://wireTapped", RECEIVE_TIMEOUT).getRight().isPresent(), is(true));
  }
}
