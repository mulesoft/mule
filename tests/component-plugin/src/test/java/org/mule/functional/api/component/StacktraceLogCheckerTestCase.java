/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.util.Arrays.asList;
import static org.mule.runtime.api.exception.MuleException.EXCEPTION_MESSAGE_SECTION_DELIMITER;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class StacktraceLogCheckerTestCase extends AbstractMuleTestCase {

  private StacktraceLogChecker stacktraceLogChecker = new StacktraceLogChecker();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();


  @Test
  public void onlyEvaluatesStacktracePatternMatchingLines() throws Exception {
    //Since there is none, the test should succeed
    String log = "message" + EXCEPTION_MESSAGE_SECTION_DELIMITER + "stacktrace";
    stacktraceLogChecker.check(log);
  }

  @Test
  public void callNotFoundShouldRaiseError() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("package","Class","method", 0);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
    stacktraceLogChecker.check("this stacktrace does not contain expected call");
  }

  @Test
  public void callFoundShouldSucceed() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("package","Class","method", 0);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:0)");
  }


  @Test
  public void linesWithoutExpectedFormatAreIgnored() throws Exception {

  }


}
