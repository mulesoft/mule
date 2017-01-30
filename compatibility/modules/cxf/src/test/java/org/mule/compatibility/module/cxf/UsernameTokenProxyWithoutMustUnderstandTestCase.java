/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.compatibility.module.cxf.wssec.ClientPasswordCallback;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class UsernameTokenProxyWithoutMustUnderstandTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public final DynamicPort httpPortProxy = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  private String request;
  private String response;

  @Override
  protected String getConfigFile() {
    return "cxf-proxy-service-without-mustunderstand-flow-httpn.xml";
  }

  @Override
  @Before
  public void doSetUp() throws Exception {
    request = IOUtils.getResourceAsString("in-message-with-mustunderstand.xml", getClass());
    response = IOUtils.getResourceAsString("out-message-with-mustunderstand.xml", getClass());
    ClientPasswordCallback.setPassword("secret");
    super.doSetUp();
    XMLUnit.setIgnoreWhitespace(true);
  }

  @Test
  public void testProxyServiceWithoutMustUnderstand() throws Exception {
    HttpRequest httpRequest =
        HttpRequest.builder().setUri("http://localhost:" + httpPortProxy.getNumber() + "/proxy-envelope")
            .setEntity(new ByteArrayHttpEntity(request.getBytes()))
            .setMethod(POST.name()).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertFalse(payload.contains("Fault"));
    assertTrue(XMLUnit.compareXML(response, payload).identical());
  }
}
