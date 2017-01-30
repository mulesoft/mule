/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.service.http.api.HttpConstants.Methods.POST;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Rule;
import org.junit.Test;

public class CxfBadSoapRequestTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  protected String getConfigFile() {
    return "soap-request-conf-flow-httpn.xml";
  }

  @Test
  public void testSoapDocumentError() throws Exception {
    String soapRequest =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "<soap:Body>" + "<ssss xmlns=\"http://www.muleumo.org\">"
            + "<request xmlns=\"http://www.muleumo.org\">Bad Request</request>" + "</ssss>" + "</soap:Body>" + "</soap:Envelope>";

    HttpRequest request = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/TestComponent")
        .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(soapRequest.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertEquals(XML.withCharset(StandardCharsets.UTF_8).toRfcString(), response.getHeaderValueIgnoreCase(CONTENT_TYPE));
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    Document document = DocumentHelper.parseText(payload);
    List<?> fault = document.selectNodes("//soap:Envelope/soap:Body/soap:Fault/faultcode");

    assertEquals(1, fault.size());
    Element faultCodeElement = (Element) fault.get(0);

    assertEquals("soap:Client", faultCodeElement.getStringValue());

    fault = document.selectNodes("//soap:Envelope/soap:Body/soap:Fault/faultstring");
    assertEquals(1, fault.size());
    Element faultStringElement = (Element) fault.get(0);
    assertEquals("Message part {http://www.muleumo.org}ssss was not recognized.  (Does it exist in service WSDL?)",
                 faultStringElement.getStringValue());
  }
}
