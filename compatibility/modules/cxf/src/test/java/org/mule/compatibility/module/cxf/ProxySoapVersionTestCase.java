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

// TODO MULE-11035 - Migrate extension tests that depend on multiple threads to use MuleArtifactFunctionalTestCase.
public class ProxySoapVersionTestCase extends AbstractCxfOverHttpExtensionTestCase {

  String doGoogleSearch =
      "<urn:doGoogleSearch xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:urn=\"urn:GoogleSearch\">";

  // Message using Soap 1.2 version
  String msgWithComment = "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">" + "<!-- comment 1 -->"
      + "<soap12:Header>" + "<!-- comment 2 -->" + "</soap12:Header>" + "<!-- comment 3 -->" + "<soap12:Body>"
      + "<!-- comment 4 -->" + doGoogleSearch + "<!-- this comment breaks it -->" + "<key>1</key>" + "<!-- comment 5 -->"
      + "<q>a</q>" + "<start>0</start>" + "<maxResults>1</maxResults>" + "<filter>false</filter>" + "<restrict>a</restrict>"
      + "<safeSearch>true</safeSearch>" + "<lr>a</lr>" + "<ie>b</ie>" + "<oe>c</oe>" + "</urn:doGoogleSearch>"
      + "<!-- comment 6 -->" + "</soap12:Body>" + "<!-- comment 7 -->" + "</soap12:Envelope>";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient();

  @Override
  protected String getConfigFile() {
    return "proxy-soap-version-conf-flow-httpn.xml";
  }

  @Test
  public void testProxyWithCommentInRequest() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/services/proxy-soap-version")
            .setMethod(POST.name())
            .setEntity(new ByteArrayHttpEntity(msgWithComment.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertTrue(payload.contains(doGoogleSearch));

  }
}
