/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.compatibility.module.cxf.CxfBasicTestCase.APP_SOAP_XML;
import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.service.http.api.HttpConstants.Methods.POST;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.xml.util.XMLUtils;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.TransformerFactoryConfigurationError;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-10618")
public class CxfBackToBlockingTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private String echoWsdl;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");
  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  protected String getConfigFile() {
    return "basic-conf-flow-httpn-nb.xml";
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
  public void backToBlocking() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");

    ParameterMap headersMap = new ParameterMap();
    headersMap.put(CONTENT_TYPE, APP_SOAP_XML.toRfcString());
    HttpRequest request = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/Echo")
        .setMethod(POST.name()).setEntity(new InputStreamHttpEntity(xml)).setHeaders(headersMap).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains("Hello!"));
    assertEquals(XML.withCharset(StandardCharsets.UTF_8), response.getHeaderValueIgnoreCase(CONTENT_TYPE));
    muleContext.getRegistry().lookupObject(SensingNullRequestResponseMessageProcessor.class).assertRequestResponseThreadsSame();
  }

  @Test
  public void backToBlockingWsdl() throws Exception {
    HttpRequest request = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/Echo" + "?wsdl")
        .setMethod(POST.name()).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    XMLUnit.compareXML(echoWsdl, payload);
    muleContext.getRegistry().lookupObject(SensingNullRequestResponseMessageProcessor.class).assertRequestResponseThreadsSame();
  }

}
