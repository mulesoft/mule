/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;


import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import org.mule.runtime.core.api.MuleEvent;

import org.junit.Test;

public class NoParamsFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "no-params-config.xml";
  }

  @Test
  public void payloadIsIgnoredOperationNoParams() throws Exception {
    MuleEvent event = flowRunner("noParams").withPayload(TEST_MESSAGE).run();
    String expectedResponse = "<ns:noParamsResponse xmlns:ns=\"http://consumer.ws.module.runtime.mule.org/\">"
        + "<text>TEST</text></ns:noParamsResponse>";
    assertXMLEqual(expectedResponse, getPayloadAsString(event.getMessage()));
  }

  @Test
  public void payloadIsIgnoredOperationNoParamsWithHeaders() throws Exception {
    String header = "<header xmlns=\"http://consumer.ws.module.runtime.mule.org/\">HEADER_VALUE</header>";
    MuleEvent event =
        flowRunner("noParamsWithHeader").withPayload(TEST_MESSAGE).withOutboundProperty("soap.header", header).run();

    String expectedResponse = "<ns2:noParamsWithHeaderResponse xmlns:ns2=\"http://consumer.ws.module.runtime.mule.org/\">"
        + "<text>HEADER_VALUE</text></ns2:noParamsWithHeaderResponse>";
    assertXMLEqual(expectedResponse, getPayloadAsString(event.getMessage()));
  }

}
