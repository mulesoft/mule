/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;


import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.module.ws.consumer.SoapFaultException;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class BasicAuthFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "basic-auth-config.xml";
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    // Change default behavior of AbstractWSConsumerFunctionalTestCase as this test only uses the new connector.
    return Arrays.asList(new Object[][] {new Object[] {false}});
  }

  @Test
  public void requestWithValidCredentialsReturnsExpectedAnswer() throws Exception {
    MuleEvent event = flowRunner("clientValidCredentials").withPayload(ECHO_REQUEST).run();
    assertXMLEqual(EXPECTED_ECHO_RESPONSE, getPayloadAsString(event.getMessage()));
    assertThat(event.getMessage().<String>getInboundProperty(HTTP_STATUS_PROPERTY), equalTo(String.valueOf(OK.getStatusCode())));
  }

  @Test
  public void requestWithInvalidCredentialsThrowsException() throws Exception {
    // The unauthorized response contains an error message in the payload (which is not a SOAP Fault), then WSConsumer
    // should fail to parse the response and throw a SoapFaultException because of this.
    SoapFaultException e =
        (SoapFaultException) flowRunner("clientInvalidCredentials").withPayload(ECHO_REQUEST).runExpectingException();
    MuleEvent event = e.getEvent();
    assertThat(event.getMessage().<String>getInboundProperty(HTTP_STATUS_PROPERTY),
               equalTo(String.valueOf(UNAUTHORIZED.getStatusCode())));
  }

  @Test
  public void requestWithInvalidCredentialsEmptyResponseThrowsException() throws Exception {
    // The unauthorized response contains an empty response, then no exception is thrown, and the payload is NullPayload.
    // The response message still contains inbound properties from the HTTP response.
    MuleEvent event = flowRunner("clientInvalidCredentialsEmptyResponse").withPayload(ECHO_REQUEST).run();
    assertThat(event.getMessage().<String>getInboundProperty(HTTP_STATUS_PROPERTY),
               equalTo(String.valueOf(UNAUTHORIZED.getStatusCode())));
  }

}
