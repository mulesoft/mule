/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static java.lang.String.format;
import static org.junit.Assert.assertNotNull;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class DatabindingTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String DATABINDING_CONF_HTTPN_XML = "databinding-conf-httpn.xml";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  protected String getConfigFile() {
    return DATABINDING_CONF_HTTPN_XML;
  }

  @Test
  public void testEchoWsdlAegisBinding() throws Exception {
    doTest("aegis");
  }

  @Test
  public void testEchoWsdlSourceBinding() throws Exception {
    doTest("source");
  }

  @Test
  public void testEchoWsdlJaxbBinding() throws Exception {
    doTest("jaxb");
  }

  @Test
  public void testEchoWsdlJibxBinding() throws Exception {
    doTest("jibx");
  }

  @Test
  public void testEchoWsdlStaxBinding() throws Exception {
    doTest("stax");
  }

  @Test
  public void testEchoWsdlCustomBinding() throws Exception {
    doTest("custom");
  }

  private void doTest(String service) throws Exception {

    HttpRequest request =
        HttpRequest.builder().setUri(format("http://localhost:%d/services/%s?wsdl", dynamicPort.getNumber(), service))
            .setMethod(POST).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertNotNull(payload);
  }
}
