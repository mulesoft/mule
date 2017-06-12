/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mule.functional.api.exception.FunctionalTestException.EXCEPTION_MESSAGE;

import org.mule.functional.api.component.FunctionalTestComponent;
import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FunctionalTestComponentTestCase extends AbstractMuleTestCase {

  FunctionalTestComponent ftc;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void initFunctionaTestComponent() {
    ftc = new FunctionalTestComponent();
    ftc.setFlowConstruct(mock(FlowConstruct.class));
    ftc.setThrowException(true);
  }

  @Test
  public void defaultExceptionWithDefaultText() throws Exception {
    checkExceptionThrown(FunctionalTestException.class, EXCEPTION_MESSAGE);
  }

  @Test
  public void defaultExceptionWithCustomText() throws Exception {
    String exceptionText = "BOOM";
    ftc.setExceptionText(exceptionText);

    checkExceptionThrown(FunctionalTestException.class, exceptionText);
  }

  @Test
  public void customExceptionWithoutText() throws Exception {
    ftc.setExceptionToThrow(IOException.class);
    checkExceptionThrown(IOException.class, null);
  }

  @Test
  public void customExceptionWithCustomText() throws Exception {
    String exceptionText = "BOOM";
    ftc.setExceptionToThrow(IOException.class);
    ftc.setExceptionText(exceptionText);
    checkExceptionThrown(IOException.class, exceptionText);
  }

  private void checkExceptionThrown(Class<? extends Exception> exceptionClass, String expectedMessage) throws MuleException {
    expected.expectCause(instanceOf(exceptionClass));
    if (expectedMessage != null) {
      expected.expectMessage(startsWith(expectedMessage));
    }

    ftc.process(null);
  }
}
