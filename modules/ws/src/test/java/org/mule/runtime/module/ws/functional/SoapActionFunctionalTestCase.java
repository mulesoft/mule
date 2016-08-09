/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class SoapActionFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  @Rule
  public ErrorCollector errorCollector = new ErrorCollector();

  @Override
  protected String getConfigFile() {
    return "soap-action-config.xml";
  }

  @Test
  public void operationWithSoapActionVersion11() throws Exception {
    assertSoapAction("operationWithSoapActionVersion11", "TestOperationWithSoapAction", null);
  }

  @Test
  public void operationWithNoSoapActionVersion11() throws Exception {
    assertSoapAction("operationWithNoSoapActionVersion11", "", null);
  }

  @Test
  public void operationWithSoapActionVersion12() throws Exception {
    assertSoapAction("operationWithSoapActionVersion12", null, "TestOperationWithSoapAction");
  }

  @Test
  public void operationWithNoSoapActionVersion12() throws Exception {
    assertSoapAction("operationWithNoSoapActionVersion12", null, null);
  }


  private void assertSoapAction(String flowName, final String expectedSoapActionHeader, final String expectedActionInContentType)
      throws Exception {
    getFunctionalTestComponent("server").setEventCallback((context, component) -> {
      String soapAction = context.getMessage().getInboundProperty("SOAPAction");

      String actionInContentType = context.getMessage().getDataType().getMediaType().getParameter("action");

      assertMatchesQuoted(expectedSoapActionHeader, soapAction);
      assertMatches(expectedActionInContentType, actionInContentType);
    });

    flowRunner(flowName).withPayload("<test/>").run();
  }

  private void assertMatchesQuoted(String expected, String value) {
    if (expected == null) {
      errorCollector.checkThat(value, nullValue());
    } else {
      errorCollector.checkThat(value, equalTo(String.format("\"%s\"", expected)));
    }
  }

  private void assertMatches(String expected, String value) {
    if (expected == null) {
      errorCollector.checkThat(value, nullValue());
    } else {
      errorCollector.checkThat(value, equalTo(expected));
    }
  }
}
