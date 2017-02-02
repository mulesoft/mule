/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.introspection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class WsdlIntrospecterTestCase {

  @Test
  public void getWsdlStyleFromOperations() {
    String resourceLocation = getResourceLocation("wsdl/document.wsdl");
    WsdlIntrospecter introspecter = new WsdlIntrospecter(resourceLocation, "Dilbert", "DilbertSoap");
    assertThat(introspecter.isDocumentStyle(), is(true));
  }

  @Test
  public void getWsdlStyleDefault() {
    String resourceLocation = getResourceLocation("wsdl/no-style-defined.wsdl");
    WsdlIntrospecter introspecter = new WsdlIntrospecter(resourceLocation, "messagingService", "messagingPort");
    assertThat(introspecter.isDocumentStyle(), is(true));
  }

  @Test
  public void getWsdlStyleFromBinding() {
    String resourceLocation = getResourceLocation("wsdl/rpc.wsdl");
    WsdlIntrospecter introspecter = new WsdlIntrospecter(resourceLocation, "SoapResponder", "SoapResponderPortType");
    assertThat(introspecter.isRpcStyle(), is(true));
  }

  private String getResourceLocation(String name) {
    return Thread.currentThread().getContextClassLoader().getResource(name).getFile();
  }
}
