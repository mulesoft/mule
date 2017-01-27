/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ProxyValidationComparisonTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

  // this request contains no spaces to check the handling of tags following the body
  private static final String ONE_LINER_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
      + "<soap:Body>" + "<echo xmlns=\"http://www.muleumo.org\">" + "<echo>hey, there!</echo>" + "</echo>" + "</soap:Body>"
      + "</soap:Envelope>";

  @Rule
  public final DynamicPort httpPort = new DynamicPort("port1");

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
    InternalMessage responseWithValidation = getResponseFor(payload + "Validation");
    InternalMessage responseWithNoValidation = getResponseFor(payload + "NoValidation");

    assertXMLEqual(getPayloadAsString(responseWithValidation), getPayloadAsString(responseWithNoValidation));
  }

  private InternalMessage getResponseFor(String path) throws MuleException {
    return muleContext.getClient().send(String.format("http://localhost:%s/services/%s", httpPort.getNumber(), path),
                                        InternalMessage.of(ONE_LINER_REQUEST), HTTP_REQUEST_OPTIONS)
        .getRight();
  }

}
