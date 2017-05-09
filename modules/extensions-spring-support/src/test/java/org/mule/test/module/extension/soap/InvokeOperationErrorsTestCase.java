/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.soap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mule.services.soap.api.exception.error.SoapErrors.BAD_REQUEST;
import static org.mule.services.soap.api.exception.error.SoapErrors.SOAP_FAULT;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.exception.MessagingException;

import org.junit.Test;

public class InvokeOperationErrorsTestCase extends SoapFootballExtensionArtifactFunctionalTestCase {

  @Test
  public void badRequest() throws Exception {
    MessagingException e = flowRunner("getLeagues").withPayload("not a valid XML").keepStreamsOpen().runExpectingException();
    Error error = e.getEvent().getError().get();
    assertThat(error.getDescription(), containsString("the request body is not a valid XML"));
    assertThat(error.getErrorType(), errorType("SOAP", BAD_REQUEST.toString()));
  }

  @Test
  public void commonSoapFault() throws Exception {
    MessagingException e = flowRunner("getLeagues").withPayload(getBodyXml("noOp", "")).keepStreamsOpen().runExpectingException();
    Error error = e.getEvent().getError().get();
    assertThat(error.getDescription(), containsString("noOp was not recognized."));
    assertThat(error.getErrorType(), errorType("SOAP", SOAP_FAULT.toString()));
  }
}
