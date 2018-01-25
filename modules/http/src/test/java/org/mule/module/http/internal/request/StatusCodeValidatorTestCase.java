/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Validates the behavior of the different status code validators in use and the inner parsing of the values, hence
 * the spaces in some of them.
 */
public class StatusCodeValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MuleEvent mockEvent = mock(MuleEvent.class);
  private MuleMessage mockMessage = mock(MuleMessage.class);
  private SuccessStatusCodeValidator successValidator = new SuccessStatusCodeValidator();
  private FailureStatusCodeValidator failureValidator = new FailureStatusCodeValidator();

  @Before
  public void setUp() {
    when(mockEvent.getMessage()).thenReturn(mockMessage);
  }

  @Test
  public void successAcceptsInRange() throws ResponseValidatorException {
    validateStatusFor(successValidator,401, "200..404");
  }

  @Test
  public void successRejectsOutRange() throws ResponseValidatorException {
    expectedException.expect(ResponseValidatorException.class);
    validateStatusFor(successValidator,500, "200.. 404");
  }

  @Test
  public void successAcceptsSpecificMatch() throws ResponseValidatorException {
    validateStatusFor(successValidator,204,"200, 204");
  }

  @Test
  public void successRejectsMismatch() throws ResponseValidatorException {
    expectedException.expect(ResponseValidatorException.class);
    validateStatusFor(successValidator,201,"200,204");
  }

  @Test
  public void successAcceptsMixedRange() throws ResponseValidatorException {
    validateStatusFor(successValidator,403,"200,204,401 ..404");
  }

  @Test
  public void successAcceptsMixedMatch() throws ResponseValidatorException {
    validateStatusFor(successValidator,204,"200, 204,401..404");
  }

  @Test
  public void successRejectsMixed() throws ResponseValidatorException {
    expectedException.expect(ResponseValidatorException.class);
    validateStatusFor(successValidator,406," 200,204,401..404");
  }

  @Test
  public void failureAcceptsOutRange() throws ResponseValidatorException {
    validateStatusFor(failureValidator, 204,"400..599");
  }

  @Test
  public void failureRejectsInRange() throws ResponseValidatorException {
    expectedException.expect(ResponseValidatorException.class);
    validateStatusFor(failureValidator, 401,"400 ..599");
  }

  @Test
  public void failureAcceptsMismatch() throws ResponseValidatorException {
    validateStatusFor(failureValidator, 403,"401,404");
  }

  @Test
  public void failureRejectsSpecificMatch() throws ResponseValidatorException {
    expectedException.expect(ResponseValidatorException.class);
    validateStatusFor(failureValidator, 401,"401 ,404");
  }

  @Test
  public void failureRejectsMixedRange() throws ResponseValidatorException {
    expectedException.expect(ResponseValidatorException.class);
    validateStatusFor(failureValidator, 403, "201,204,401 ..404");
  }

  @Test
  public void failureRejectsMixedMatch() throws ResponseValidatorException {
    expectedException.expect(ResponseValidatorException.class);
    validateStatusFor(failureValidator, 204,"200, 204,401..404");
  }

  @Test
  public void failureAcceptsMixed() throws ResponseValidatorException {
    validateStatusFor(failureValidator, 406," 200,204,401..404");
  }

  private void validateStatusFor(RangeStatusCodeValidator validator, int status, String values) throws ResponseValidatorException {
    when(mockMessage.getInboundProperty(HTTP_STATUS_PROPERTY)).thenReturn(status);
    validator.setValues(values);
    validator.validate(mockEvent);
  }

}
