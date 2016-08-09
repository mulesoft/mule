/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.ws.functional;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class WSConsumerFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  @Parameterized.Parameter(value = 0)
  public String configFile;

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    // TODO add test cases for JMS once MULE-9210 gets fixed
    return Arrays.asList(new Object[] {"ws-consumer-http-module-config.xml"}, new Object[] {"ws-consumer-http-module-config.xml"}

    );
  }

  @Test
  public void validRequestReturnsExpectedAnswer() throws Exception {
    assertValidResponse("client");
  }

  @Test
  public void invalidRequestFormatReturnsSOAPFault() throws Exception {
    String message = "<tns:echo xmlns:tns=\"http://consumer.ws.module.runtime.mule.org/\"><invalid>Hello</invalid></tns:echo>";
    assertSoapFault("client", message, "Client");
  }

  @Test
  public void invalidNamespaceReturnsSOAPFault() throws Exception {
    String message = "<tns:echo xmlns:tns=\"http://invalid/\"><text>Hello</text></tns:echo>";
    assertSoapFault("client", message, "Client");
  }

}
