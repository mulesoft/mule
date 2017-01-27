/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.module.cxf;

import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

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

public class ProxyRPCBindingTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public final DynamicPort httpPortProxy = new DynamicPort("port1");

  @Rule
  public final DynamicPort httpPortService = new DynamicPort("port2");

  private String getAllRequest;
  private String getAllResponse;
  @Rule
  public TestHttpClient httpClient = new TestHttpClient();

  @Override
  protected String getConfigFile() {
    return "proxy-rpc-binding-conf-httpn.xml";
  }

  @Override
  @Before
  public void doSetUp() throws Exception {
    getAllRequest = IOUtils.getResourceAsString("artistregistry-get-all-request.xml", getClass());
    getAllResponse = IOUtils.getResourceAsString("artistregistry-get-all-response.xml", getClass());
    XMLUnit.setIgnoreWhitespace(true);
  }

  @Test
  public void proxyRPCBodyPayload() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + httpPortProxy.getNumber() + "/body")
            .setMethod(POST.name())
            .setEntity(new ByteArrayHttpEntity(getAllRequest.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    assertTrue(XMLUnit.compareXML(getAllResponse, payload).identical());
  }

  @Test
  public void proxyRPCBodyEnvelope() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + httpPortProxy.getNumber() + "/envelope")
            .setMethod(POST.name())
            .setEntity(new ByteArrayHttpEntity(getAllRequest.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());

    assertTrue(XMLUnit.compareXML(getAllResponse, payload).identical());
  }

}
