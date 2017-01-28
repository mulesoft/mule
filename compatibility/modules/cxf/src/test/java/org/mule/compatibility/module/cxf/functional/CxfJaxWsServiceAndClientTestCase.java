/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.functional;


import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.Methods.POST;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;

import org.mule.compatibility.module.cxf.AbstractCxfOverHttpExtensionTestCase;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class CxfJaxWsServiceAndClientTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String REQUEST_PAYLOAD =
      "<soap:Envelope \n" + "           xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
          + "           xmlns:svc=\"http://example.cxf.module.compatibility.mule.org/\">\n" + "<soap:Body>\n" + "<svc:sayHi>\n"
          + "    <arg0>Test Message</arg0>\n" + "</svc:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  private static final String RESPONSE_PAYLOAD = "<soap:Envelope " + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
      + "<soap:Body>" + "<ns2:sayHiResponse xmlns:ns2=\"http://example.cxf.module.compatibility.mule.org/\">" + "<return>"
      + "Hello\u2297 Test Message" + "</return>" + "</ns2:sayHiResponse>" + "</soap:Body>" + "</soap:Envelope>";

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient();

  @Override
  protected String getConfigFile() {
    return "cxf-jaxws-service-and-client-config-httpn.xml";
  }

  @Test
  public void jaxWsClientReadsMuleMethodPropertySetByJaxWsService() throws Exception {
    HttpRequest httpRequest = HttpRequest.builder().setUri("http://localhost:" + port.getNumber() + "/hello")
        .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(REQUEST_PAYLOAD.getBytes())).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertEquals(RESPONSE_PAYLOAD, payload);
  }

  @Test
  public void jaxWsServerWithMtoMServiceHasCorrectContentType() throws Exception {
    HttpRequest httpRequest = HttpRequest.builder().setUri("http://localhost:" + port.getNumber() + "/helloMtoM")
        .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(REQUEST_PAYLOAD.getBytes())).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    assertThat(httpResponse.getHeaderValueIgnoreCase(CONTENT_TYPE),
               allOf(startsWith("multipart/related; charset=UTF-8; boundary=\"uuid:"),
                     endsWith("\"; start=\"<root.message@cxf.apache.org>\"; type=\"application/xop+xml\"; start-info=\"text/xml\"")));

    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertThat(payload, containsString(RESPONSE_PAYLOAD));
  }
}
