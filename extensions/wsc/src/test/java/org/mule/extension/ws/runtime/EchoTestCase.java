/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.ECHO_ACCOUNT;
import static org.mule.extension.ws.WscTestUtils.ECHO_HEADERS;
import static org.mule.extension.ws.WscTestUtils.HEADER_INOUT;
import static org.mule.extension.ws.WscTestUtils.HEADER_OUT;
import static org.mule.extension.ws.WscTestUtils.assertSoapResponse;
import org.mule.extension.ws.AbstractSoapServiceTestCase;
import org.mule.extension.ws.api.WscAttributes;
import org.mule.runtime.api.message.Message;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("Operation Execution")
public class EchoTestCase extends AbstractSoapServiceTestCase {

  // Flow Names
  private static final String ECHO_FLOW = "echoOperation";
  private static final String ECHO_HEADERS_FLOW = "echoWithHeadersOperation";
  private static final String ECHO_ACCOUNT_FLOW = "echoAccountOperation";

  @Override
  protected String getConfigFile() {
    return "config/echo.xml";
  }

  @Test
  @Description("Consumes an operation that expects a simple type and returns a simple type")
  public void echoOperation() throws Exception {
    Message message = runFlowWithRequest(ECHO_FLOW, ECHO);
    String out = (String) message.getPayload().getValue();
    assertSoapResponse(ECHO, out);
  }

  @Test
  @Description("Consumes an operation that expects an input and a set of headers and returns a simple type and a set of headers")
  public void echoWithHeadersOperation() throws Exception {
    Message message = runFlowWithRequest(ECHO_HEADERS_FLOW, ECHO_HEADERS);

    String out = (String) message.getPayload().getValue();
    assertSoapResponse(ECHO_HEADERS, out);

    WscAttributes attributes = (WscAttributes) message.getAttributes();
    List<String> headers = new ArrayList<>(attributes.getSoapHeaders().values());
    assertThat(headers, hasSize(2));
    assertSoapResponse(HEADER_INOUT, headers.get(0));
    assertSoapResponse(HEADER_OUT, headers.get(1));
  }

  @Test
  @Description("Consumes an operation that expects 2 parameters (a simple one and a complex one) and returns a complex type")
  public void echoAccountOperation() throws Exception {
    Message message = runFlowWithRequest(ECHO_ACCOUNT_FLOW, ECHO_ACCOUNT);
    String out = (String) message.getPayload().getValue();
    assertSoapResponse(ECHO_ACCOUNT, out);
    WscAttributes attributes = (WscAttributes) message.getAttributes();
    assertThat(attributes.getSoapHeaders().isEmpty(), is(true));
  }
}
