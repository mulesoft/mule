/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static java.lang.String.format;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;

public class ProxyValidationComparisonTestCase extends AbstractCxfOverHttpExtensionTestCase {

  // this request contains no spaces to check the handling of tags following the body
  private static final String ONE_LINER_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
      + "<soap:Body>" + "<echo xmlns=\"http://www.muleumo.org\">" + "<echo>hey, there!</echo>" + "</echo>" + "</soap:Body>"
      + "</soap:Envelope>";

  @Rule
  public final DynamicPort httpPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient();

  @Override
  protected String getConfigFile() {
    return "proxy-validation-comparison-config-httpn.xml";
  }

  @Test
  public void responsesAreEqualWithAndWithoutValidationEnvelope() throws Exception {
    testResponsesWithPayload("envelope");
  }

  @Test
  public void responsesAreEqualWithAndWithoutValidationBody() throws Exception {
    testResponsesWithPayload("body");
  }

  private void testResponsesWithPayload(String payload) throws Exception {
    HttpResponse responseWithValidation = getResponseFor(payload + "Validation");
    HttpResponse responseWithNoValidation = getResponseFor(payload + "NoValidation");

    String responsePayloadWithValidation =
        IOUtils.toString(((InputStreamHttpEntity) responseWithValidation.getEntity()).getInputStream());
    String responsePayloadWithoutValidation =
        IOUtils.toString(((InputStreamHttpEntity) responseWithNoValidation.getEntity()).getInputStream());
    assertXMLEqual(responsePayloadWithValidation, responsePayloadWithoutValidation);
  }

  private HttpResponse getResponseFor(String path) throws MuleException, IOException, TimeoutException {
    HttpRequest request = HttpRequest.builder().setUri(format("http://localhost:%s/services/%s", httpPort.getNumber(), path))
        .setMethod(POST.name()).setEntity(new ByteArrayHttpEntity(ONE_LINER_REQUEST.getBytes())).build();

    return httpClient.send(request, RECEIVE_TIMEOUT, false, null);
  }

}
