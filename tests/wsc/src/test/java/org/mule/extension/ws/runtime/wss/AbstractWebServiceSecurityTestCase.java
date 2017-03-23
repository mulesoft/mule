/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime.wss;

import static java.lang.String.format;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.assertSoapResponse;
import org.mule.extension.ws.AbstractSoapServiceTestCase;
import org.mule.runtime.api.message.Message;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

public abstract class AbstractWebServiceSecurityTestCase extends AbstractSoapServiceTestCase {

  private final String WSS_CONFIG_FILE_MASK = "config/wss/%s.xml";

  private final String security;

  AbstractWebServiceSecurityTestCase(String security) {
    this.security = security;
  }

  @Override
  protected String getConfigurationFile() {
    return format(WSS_CONFIG_FILE_MASK, security);
  }

  protected String getTestName() {
    return security;
  }

  @Test
  @Description("Consumes a simple operation of a secured web service and expects a valid response")
  public void expectedSecuredRequest() throws Exception {
    Message message = runFlowWithRequest(security + "Flow", ECHO);
    String out = (String) message.getPayload().getValue();
    assertSoapResponse(ECHO, out);
  }
}
