/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.junit.Assert.assertTrue;
import static org.mule.extension.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ProxyWithValidationTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

  public static final String SAMPLE_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
      + "<soap:Body> " + "<echo xmlns=\"http://www.muleumo.org\">" + "  <echo><![CDATA[bla]]></echo>" + "</echo>" + "</soap:Body>"
      + "</soap:Envelope>";

  @Rule
  public final DynamicPort httpPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "proxy-with-validation-config-httpn.xml";
  }

  @Test
  public void acceptsRequestWithCData() throws Exception {
    InternalMessage response = muleContext.getClient().send("http://localhost:" + httpPort.getNumber() + "/services/Echo",
                                                            InternalMessage.of(SAMPLE_REQUEST), HTTP_REQUEST_OPTIONS)
        .getRight();

    assertTrue(getPayloadAsString(response).contains("bla"));
  }
}
