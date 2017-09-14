/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.exception.MuleException.EXCEPTION_MESSAGE_DELIMITER;
import static org.mule.runtime.api.exception.MuleException.EXCEPTION_MESSAGE_SECTION_DELIMITER;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EqualsLogCheckerTestCase extends AbstractMuleTestCase {

  private EqualsLogChecker equalsLogChecker = new EqualsLogChecker();

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void onlyEvaluatesMessage() throws Exception {
    String log = format("message%s" +
        "%s" +
        "stacktrace", lineSeparator(), EXCEPTION_MESSAGE_SECTION_DELIMITER);
    equalsLogChecker.setExpectedLogMessage("message");
    equalsLogChecker.check(log);
  }

  @Test
  public void stacktraceShouldBeAvoidedEvenWithNoDelimiter() throws Exception {
    String log = format("first line%s" +
        "second line%s" +
        "at some.package.SomeClass.theMethod(theline:56)", lineSeparator(), lineSeparator());
    equalsLogChecker.setExpectedLogMessage(format("first line%s" +
        "second line", lineSeparator()));
    equalsLogChecker.check(log);
  }

  @Test
  public void whitespaceDifferencesDontCauseFailureIfFilterSet() throws Exception {
    String log = format("  \t%s" +
        "%smessage%s" +
        "%s" +
        "\t  ", lineSeparator(), lineSeparator(), lineSeparator(), lineSeparator());
    equalsLogChecker.setShouldFilterLogMessage(true);
    equalsLogChecker.setExpectedLogMessage("message");
    equalsLogChecker.check(log);
  }

  @Test
  public void whitepaceDifferencesCauseFailureIfFilterNotSet() throws Exception {
    String log = format("  \t%s" +
        "%smessage%s" +
        "%s" +
        "\t  ", lineSeparator(), lineSeparator(), lineSeparator(), lineSeparator());
    equalsLogChecker.setShouldFilterLogMessage(false);
    equalsLogChecker.setExpectedLogMessage("message");
    expectedException.expect(AssertionError.class);
    equalsLogChecker.check(log);
  }

  @Test
  public void whitespaceDifferencesBeforeAndAfterSucceedWithAndWithoutFilter() throws Exception {
    String log = format("first line%s" +
        "   second line   %s" +
        "\tthird line\t", lineSeparator(), lineSeparator());
    equalsLogChecker.setExpectedLogMessage(format("first line%s" +
        "second line%s" +
        "third line", lineSeparator(), lineSeparator()));
    equalsLogChecker.setShouldFilterLogMessage(false);
    equalsLogChecker.check(log);
    equalsLogChecker.setShouldFilterLogMessage(true);
    equalsLogChecker.check(log);
  }

  @Test
  public void delimiterDifferencesShouldSucceedWithFilter() throws Exception {
    String log = EXCEPTION_MESSAGE_DELIMITER + "first line" + lineSeparator() + EXCEPTION_MESSAGE_DELIMITER + "second line";
    equalsLogChecker.setShouldFilterLogMessage(true);
    equalsLogChecker.setExpectedLogMessage(format("first line%s" +
        "second line", lineSeparator()));
    equalsLogChecker.check(log);
  }

  @Test
  public void delimiterDifferencesShouldFailWithoutFilter() throws Exception {
    String log = EXCEPTION_MESSAGE_DELIMITER + "first line" + lineSeparator() + EXCEPTION_MESSAGE_DELIMITER + "second line";
    equalsLogChecker.setShouldFilterLogMessage(false);
    equalsLogChecker.setExpectedLogMessage(format("first line%s" +
        "second line", lineSeparator()));
    expectedException.expect(AssertionError.class);
    equalsLogChecker.check(log);
  }

  @Test
  public void filtersMessageFromStacktraceProperly() throws Exception {
    String logMessage = format("first line%s" +
        "second line%s", lineSeparator(), lineSeparator());
    Exception ex1 = new Exception("exception in layer 1");
    Exception ex2 = new Exception("exception in layer 2", ex1);
    Exception ex3 = new Exception("exception in layer 3", ex2);
    StringWriter s = new StringWriter();
    PrintWriter p = new PrintWriter(s);
    ex3.printStackTrace(p);
    equalsLogChecker.setExpectedLogMessage(logMessage);
    equalsLogChecker.check(logMessage + s.toString());
  }

}
