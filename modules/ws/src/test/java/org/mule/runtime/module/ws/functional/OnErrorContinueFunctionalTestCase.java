/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;


import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import org.mule.functional.listener.ExceptionListener;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.module.ws.consumer.SoapFaultException;

import org.apache.cxf.binding.soap.SoapFault;
import org.junit.Test;

public class OnErrorContinueFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  private static final String FAIL_REQUEST =
      "<tns:fail xmlns:tns=\"http://consumer.ws.module.runtime.mule.org/\">" + "<text>Hello</text></tns:fail>";

  private static final String EXPECTED_SOAP_FAULT_DETAIL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><detail>"
      + "<ns2:EchoException xmlns:ns2=\"http://consumer.ws.module.runtime.mule.org/\">"
      + "<text>Hello</text></ns2:EchoException></detail>";

  @Override
  protected String getConfigFile() {
    return "on-error-continue-config.xml";
  }

  @Test
  public void soapFaultThrowsException() throws Exception {
    MessagingException e = flowRunner("soapFaultWithoutCatchExceptionStrategy").withPayload(FAIL_REQUEST).runExpectingException();

    assertThat(e.getEvent().getError().isPresent(), is(true));

    SoapFaultException soapFault = (SoapFaultException) e.getEvent().getError().get().getCause();
    assertThat(soapFault.getMessage(), startsWith("Hello"));
    assertThat(soapFault.getFaultCode().getLocalPart(), is("Server"));
    assertThat(soapFault.getCause(), instanceOf(SoapFault.class));
  }

  @Test
  public void catchExceptionStrategyHandlesSoapFault() throws Exception {
    ExceptionListener listener = new ExceptionListener(muleContext);
    MuleEvent event = flowRunner("soapFaultWithCatchExceptionStrategy").withPayload(FAIL_REQUEST).run();

    // Assert that the exception was thrown
    listener.waitUntilAllNotificationsAreReceived();

    assertXMLEqual(EXPECTED_SOAP_FAULT_DETAIL, getPayloadAsString(event.getMessage()));

    assertThat(event.getError().isPresent(), is(false));

    SoapFaultException soapFault = ((InternalMessage) event.getMessage()).getOutboundProperty("soapFaultException");
    assertThat(soapFault.getMessage(), startsWith("Hello"));
    assertThat(soapFault.getFaultCode().getLocalPart(), is("Server"));
    assertThat(soapFault.getCause(), instanceOf(SoapFault.class));
  }

}
