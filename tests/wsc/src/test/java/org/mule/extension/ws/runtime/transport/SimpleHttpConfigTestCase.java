/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime.transport;

import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.assertSoapResponse;
import static org.mule.test.allure.AllureConstants.WscFeature.WSC_EXTENSION;
import org.mule.extension.ws.AbstractSoapServiceTestCase;
import org.mule.runtime.api.message.Message;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(WSC_EXTENSION)
@Stories({"Operation Execution", "Custom Transport", "Http"})
public class SimpleHttpConfigTestCase extends AbstractSoapServiceTestCase {

  @Test
  public void simpleConfigNoAuthentication() throws Exception {
    Message message = runFlowWithRequest("simpleRequesterConfig", ECHO);
    String out = (String) message.getPayload().getValue();
    assertSoapResponse(ECHO, out);
  }

  @Test
  public void soapFaultWithCustomTransport() {

  }

  @Override
  protected String getConfigurationFile() {
    return "config/transport/simple-http-custom-transport.xml";
  }
}
