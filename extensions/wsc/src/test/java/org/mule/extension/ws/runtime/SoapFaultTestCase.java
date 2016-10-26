/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.ws.WscTestUtils.ECHO_HEADERS_XML;
import static org.mule.extension.ws.WscTestUtils.FAIL_XML;
import static org.mule.extension.ws.WscTestUtils.resourceAsString;
import org.mule.extension.ws.WebServiceConsumerTestCase;
import org.mule.extension.ws.api.exception.SoapFaultException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

@ArtifactClassLoaderRunnerConfig(testInclusions = {"org.apache.cxf.*:*:*:*:*"})
public class SoapFaultTestCase extends WebServiceConsumerTestCase {

  private static final String FAIL_FLOW = "failOperation";

  @Override
  protected String getConfigFile() {
    return "config/fail.xml";
  }

  @Test
  @Description("Consumes an operation that throws a SOAP Fault and expects a Soap Fault Exception")
  public void failOperation() throws Exception {
    MessagingException me = flowRunner(FAIL_FLOW).withPayload(resourceAsString("request/" + FAIL_XML)).runExpectingException();
    Exception causeException = me.getCauseException();
    assertThat(causeException, instanceOf(SoapFaultException.class));
    SoapFaultException sf = (SoapFaultException) causeException;
    assertThat(sf.getFaultCode().getLocalPart(), is("Server"));
    assertThat(sf.getMessage(), is("Fail Message"));
  }

  @Test
  @Description("Consumes an operation that expects a set of headers and that throws a SOAP Fault because they are not provided")
  public void missingHeadersOperation() throws Exception {
    MessagingException me = flowRunner("echoMissingHeaders")
        .withPayload(resourceAsString("request/" + ECHO_HEADERS_XML))
        .runExpectingException();
    Exception causeException = me.getCauseException();
    assertThat(causeException, instanceOf(SoapFaultException.class));
    SoapFaultException sf = (SoapFaultException) causeException;
    assertThat(sf.getFaultCode().getLocalPart(), is("Server"));
    assertThat(sf.getMessage(), is("Missing Required Headers"));
  }
}
