/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static java.lang.String.format;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class DatabindingTestCase extends FunctionalTestCase {

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).build();
  private static final String DATABINDING_CONF_HTTPN_XML = "databinding-conf-httpn.xml";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

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
    MuleMessage result =
        muleContext.getClient().send(format("http://localhost:%d/services/%s?wsdl", dynamicPort.getNumber(), service),
                                     MuleMessage.builder().nullPayload().build(), HTTP_REQUEST_OPTIONS)
            .getRight();
    assertNotNull(result.getPayload());
  }
}
