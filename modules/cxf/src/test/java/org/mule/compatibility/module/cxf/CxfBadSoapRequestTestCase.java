/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.extension.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Rule;
import org.junit.Test;

public class CxfBadSoapRequestTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

  @Override
  protected String getConfigFile() {
    return "soap-request-conf-flow-httpn.xml";
  }

  @Test
  public void testSoapDocumentError() throws Exception {
    MuleClient client = muleContext.getClient();

    String soapRequest =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "<soap:Body>" + "<ssss xmlns=\"http://www.muleumo.org\">"
            + "<request xmlns=\"http://www.muleumo.org\">Bad Request</request>" + "</ssss>" + "</soap:Body>" + "</soap:Envelope>";

    InternalMessage reply = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/TestComponent",
                                        InternalMessage.of(soapRequest), HTTP_REQUEST_OPTIONS)
        .getRight();

    assertNotNull(reply);
    assertNotNull(reply.getPayload().getValue());

    String ct = reply.getPayload().getDataType().getMediaType().toRfcString();
    assertEquals("text/xml; charset=UTF-8", ct);

    Document document = DocumentHelper.parseText(getPayloadAsString(reply));
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
