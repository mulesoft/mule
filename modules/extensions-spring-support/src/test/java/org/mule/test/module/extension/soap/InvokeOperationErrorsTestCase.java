/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.soap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.mule.services.soap.api.exception.error.SoapErrors.BAD_REQUEST;
import static org.mule.services.soap.api.exception.error.SoapErrors.SOAP_FAULT;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.exception.MessagingException;

import java.util.Optional;

import org.junit.Test;

public class InvokeOperationErrorsTestCase extends SoapExtensionArtifactFunctionalTestCase {

  @Test
  public void badRequest() throws Exception {
    MessagingException e = flowRunner("getLeagues").withPayload("not a valid XML").keepStreamsOpen().runExpectingException();
    Optional<Error> error = e.getEvent().getError();
    assertThat(error.isPresent(), is(true));
    assertThat(error.get().getDescription(), containsString("the request body is not a valid XML"));
    assertThat(error.get().getErrorType().getNamespace(), is("SOAP"));
    assertThat(error.get().getErrorType().getIdentifier(), is(BAD_REQUEST.toString()));
  }

  @Test
  public void commonSoapFault() throws Exception {
    MessagingException e = flowRunner("getLeagues").withPayload(getBodyXml("noOp", "")).keepStreamsOpen().runExpectingException();
    Optional<Error> error = e.getEvent().getError();
    assertThat(error.isPresent(), is(true));
    assertThat(error.get().getDescription(), containsString("noOp was not recognized."));
    assertThat(error.get().getErrorType().getNamespace(), is("SOAP"));
    assertThat(error.get().getErrorType().getIdentifier(), is(SOAP_FAULT.toString()));
  }
}
