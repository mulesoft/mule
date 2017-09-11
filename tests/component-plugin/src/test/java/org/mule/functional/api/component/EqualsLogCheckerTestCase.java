/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static org.mule.runtime.api.exception.MuleException.EXCEPTION_MESSAGE_DELIMITER;
import static org.mule.runtime.api.exception.MuleException.EXCEPTION_MESSAGE_SECTION_DELIMITER;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EqualsLogCheckerTestCase extends AbstractMuleTestCase {

  private EqualsLogChecker equalsLogChecker = new EqualsLogChecker();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void onlyEvaluatesMessage() throws Exception {
    String log = "message\n" + EXCEPTION_MESSAGE_SECTION_DELIMITER + "stacktrace";
    equalsLogChecker.setExpectedLogMessage("message");
    equalsLogChecker.check(log);
  }

  @Test
  public void stacktraceShouldBeAvoidedEvenWithNoDelimiter() throws Exception {
    String log = "first line\nsecond line\nat some.package.SomeClass.theMethod(theline:56)";
    equalsLogChecker.setExpectedLogMessage("first line\nsecond line");
    equalsLogChecker.check(log);
  }

  @Test
  public void whitespaceDifferencesDontCauseFailureIfFilterSet() throws Exception {
    String log = "  \t\n\nmessage\n\n\t  ";
    equalsLogChecker.setShouldFilterLogMessage(true);
    equalsLogChecker.setExpectedLogMessage("message");
    equalsLogChecker.check(log);
  }

  @Test
  public void whitepaceDifferencesCauseFailureIfFilterNotSet() throws Exception {
    String log = "  \t\n\nmessage\n\n\t  ";
    equalsLogChecker.setShouldFilterLogMessage(false);
    equalsLogChecker.setExpectedLogMessage("message");
    expectedException.expect(AssertionError.class);
    equalsLogChecker.check(log);
  }

  @Test
  public void whitespaceDifferencesBeforeAndAfterSucceedWithAndWithoutFilter() throws Exception {
    String log = "first line\n   second line   \n\tthird line\t";
    equalsLogChecker.setExpectedLogMessage("first line\nsecond line\nthird line");
    equalsLogChecker.setShouldFilterLogMessage(false);
    equalsLogChecker.check(log);
    equalsLogChecker.setShouldFilterLogMessage(true);
    equalsLogChecker.check(log);
  }

  @Test
  public void delimiterDifferencesShouldSucceedWithFilter() throws Exception {
    String log = EXCEPTION_MESSAGE_DELIMITER + "first line\n" + EXCEPTION_MESSAGE_DELIMITER + "second line";
    equalsLogChecker.setShouldFilterLogMessage(true);
    equalsLogChecker.setExpectedLogMessage("first line\nsecond line");
    equalsLogChecker.check(log);
  }

  @Test
  public void delimiterDifferencesShouldFailWithoutFilter() throws Exception {
    String log = EXCEPTION_MESSAGE_DELIMITER + "first line\n" + EXCEPTION_MESSAGE_DELIMITER + "second line";
    equalsLogChecker.setShouldFilterLogMessage(false);
    equalsLogChecker.setExpectedLogMessage("first line\nsecond line");
    expectedException.expect(AssertionError.class);
    equalsLogChecker.check(log);
  }

}
