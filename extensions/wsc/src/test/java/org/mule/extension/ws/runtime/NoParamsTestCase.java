/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime;

import static org.mule.extension.ws.WscTestUtils.NO_PARAMS_XML;
import org.mule.extension.ws.WebServiceConsumerTestCase;
import org.mule.runtime.api.message.Message;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

public class NoParamsTestCase extends WebServiceConsumerTestCase {

  private static final String NO_PARAMS_FLOW = "noParamsOperation";

  @Override
  protected String getConfigFile() {
    return "config/noParams.xml";
  }

  @Test
  @Description("Consumes an operation that expects no parameters and returns a simple type")
  public void noParamsOperation() throws Exception {
    Message message = runFlowWithRequest(NO_PARAMS_FLOW, NO_PARAMS_XML);
    String payload = (String) message.getPayload().getValue();
    assertSoapResponse(NO_PARAMS_XML, payload);
  }
}
