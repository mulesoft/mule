/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ProxyWithValidationTestCase extends AbstractCxfOverHttpExtensionTestCase {

  public static final String SAMPLE_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
      + "<soap:Body> " + "<echo xmlns=\"http://www.muleumo.org\">" + "  <echo><![CDATA[bla]]></echo>" + "</echo>" + "</soap:Body>"
      + "</soap:Envelope>";

  @Rule
  public final DynamicPort httpPort = new DynamicPort("port1");
  @Rule
  public TestHttpClient httpClient = new TestHttpClient();

  @Override
  protected String getConfigFile() {
    return "proxy-with-validation-config-httpn.xml";
  }

  @Test
  public void acceptsRequestWithCData() throws Exception {
    HttpRequest request = HttpRequest.builder().setUri("http://localhost:" + httpPort.getNumber() + "/services/Echo")
        .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(SAMPLE_REQUEST.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains("bla"));
  }
}
