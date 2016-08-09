/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;


import static java.util.Collections.singletonMap;

import org.junit.Test;

public class DynamicAddressFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "dynamic-address-config.xml";
  }

  @Test
  public void returnsExpectedResponseWhenValidPathIsProvidedInboundProperty() throws Exception {
    assertValidResponse("clientInboundProperty", ECHO_REQUEST, singletonMap("pathInboundProperty", "services/Test"));
  }

  @Test
  public void returnsExpectedResponseWhenValidPathIsProvidedOutboundProperty() throws Exception {
    assertValidResponse("clientOutboundProperty");
  }

  @Test
  public void returnsExpectedResponseWhenValidPathIsProvidedFlowVar() throws Exception {
    assertValidResponse("clientFlowVar");
  }

  @Test
  public void failsWhenInvalidPathIsProvided() throws Exception {
    assertSoapFault("clientInboundProperty", ECHO_REQUEST, singletonMap("clientInboundProperty", "invalid"), "Client");
  }

  @Test
  public void failsWhenNoPathIsDefined() throws Exception {
    assertSoapFault("clientInboundProperty", ECHO_REQUEST, "Client");
  }
}
