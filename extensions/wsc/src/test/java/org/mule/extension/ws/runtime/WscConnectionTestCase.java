/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime;

import static org.mule.extension.ws.WscTestUtils.ECHO_XML;
import static org.mule.extension.ws.WscTestUtils.resourceAsString;
import org.mule.extension.ws.WebServiceConsumerTestCase;
import org.mule.runtime.api.message.Message;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

public class WscConnectionTestCase extends WebServiceConsumerTestCase {

  private static final String SAME_INSTANCE_FLOW = "operationShareInstance";

  @Override
  protected String getConfigFile() {
    return "config/connection.xml";
  }

  @Test
  @Description("Consumes 2 operations sharing the same connection instance")
  public void sameConnection() throws Exception {
    Message msg = flowRunner(SAME_INSTANCE_FLOW).withVariable("req", resourceAsString("request/" + ECHO_XML)).run().getMessage();
    String out = (String) msg.getPayload().getValue();
    assertSoapResponse(ECHO_XML, out);
  }

}
