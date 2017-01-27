/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.xml.util.XMLUtils;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;

import javax.xml.transform.TransformerFactoryConfigurationError;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;

public class CxfBasicTestCase extends AbstractCxfOverHttpExtensionTestCase {

  public static final MediaType APP_SOAP_XML = MediaType.create("application", "soap+xml");

  private String echoWsdl;

  @Rule
  public TestHttpClient httpClient = new TestHttpClient();

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "basic-conf-flow-httpn.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    echoWsdl = IOUtils.getResourceAsString("cxf-echo-service.wsdl", getClass());
    XMLUnit.setIgnoreWhitespace(true);
    try {
      XMLUnit.getTransformerFactory();
    } catch (TransformerFactoryConfigurationError e) {
      XMLUnit.setTransformerFactory(XMLUtils.TRANSFORMER_FACTORY_JDK5);
    }
  }

  @Test
  public void testEchoService() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/Echo").setMethod(POST.name())
            .setEntity(new InputStreamHttpEntity(xml)).addHeader("content-type", APP_SOAP_XML.toRfcString()).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    assertTrue(payload.contains("Hello!"));
    assertEquals("text/xml; charset=UTF-8", response.getHeaderValue("content-type"));
  }

  @Test
  public void testEchoWsdl() throws Exception {
    HttpRequest request = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/Echo" + "?wsdl")
        .setMethod(POST.name()).build();
    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    XMLUnit.compareXML(echoWsdl, payload);
  }
}
