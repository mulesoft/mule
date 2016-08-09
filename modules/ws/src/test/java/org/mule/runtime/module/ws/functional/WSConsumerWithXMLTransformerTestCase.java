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

public class WSConsumerWithXMLTransformerTestCase extends AbstractWSConsumerFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "ws-consumer-with-xml-transformer-config.xml";
  }

  @Test
  public void consumerWorksWithXMLTransformer() throws Exception {
    MuleEvent response = flowRunner("client").withPayload(ECHO_REQUEST).run();
    assertXMLEqual(EXPECTED_ECHO_RESPONSE, getPayloadAsString(response.getMessage()));
  }
}
