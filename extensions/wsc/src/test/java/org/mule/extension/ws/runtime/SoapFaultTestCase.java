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
import static org.mule.extension.ws.WscTestUtils.FAIL;
import static org.mule.extension.ws.WscTestUtils.UPLOAD_SINGLE_ATT;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import org.mule.extension.ws.AbstractSoapServiceTestCase;
import org.mule.extension.ws.api.exception.SoapFaultException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

@ArtifactClassLoaderRunnerConfig(testInclusions = {"org.apache.cxf.*:*:*:*:*"})
public class SoapFaultTestCase extends AbstractSoapServiceTestCase {

  private static final String FAIL_FLOW = "failOperation";

  @Override
  protected String getConfigFile() {
    return "config/fail.xml";
  }

  @Test
  @Description("Consumes an operation that throws a SOAP Fault and expects a Soap Fault Exception")
  public void failOperation() throws Exception {
    MessagingException me = flowRunner(FAIL_FLOW).withPayload(getRequestResource(FAIL)).runExpectingException();
    Exception causeException = me.getCauseException();
    assertThat(causeException, instanceOf(SoapFaultException.class));
    SoapFaultException sf = (SoapFaultException) causeException;
    assertThat(sf.getFaultCode().getLocalPart(), is("Server"));
    assertThat(sf.getMessage(), is("Fail Message"));
  }

  @Test
  @Description("Consumes an operation that does not exist and throws a SOAP Fault because of it and asserts the thrown exception")
  public void missingHeadersOperation() throws Exception {
    MessagingException e = flowRunner(FAIL_FLOW).withPayload(getRequestResource(UPLOAD_SINGLE_ATT)).runExpectingException();
    Exception causeException = e.getCauseException();
    assertThat(causeException, instanceOf(SoapFaultException.class));
    SoapFaultException sf = (SoapFaultException) causeException;
    assertThat(sf.getFaultCode().getLocalPart(), is("Client"));
    assertThat(sf.getMessage(), is("Cannot find dispatch method for {http://consumer.ws.extension.mule.org/}uploadAttachment"));
  }
}
