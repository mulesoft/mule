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
  public void stacktraceShouldBeEvalatedEvenWithNoSectionDelimiter() throws Exception {
    String log = "this could be\nthe message part and\tshould not be evaluated\nat package.Class.method(whatever:0)";
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("package", "Class", "method", 0);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check(log);
  }

  @Test
  public void callNotFoundShouldRaiseError() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("package", "Class", "method", 0);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
    stacktraceLogChecker.check("this stacktrace does not contain expected call");
  }

  @Test
  public void callFoundShouldSucceed() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("package", "Class", "method", 0);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:0)");
  }


  @Test
  public void noLineNumberSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("package", "Class", "method");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void differentLineNumbersRepresentDifferentExpectedMethodCalls() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("package", "Class", "method", 0);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void methodWithNumbersInNameIsFound() throws Exception {
    stacktraceLogChecker.setExpectedCalls(asList(
                                                 new StacktraceLogChecker.MethodCall("package", "Class", "method0", 0),
                                                 new StacktraceLogChecker.MethodCall("package", "Class", "0method", 1),
                                                 new StacktraceLogChecker.MethodCall("package", "Class", "method0method", 2)));
    stacktraceLogChecker
        .check("at package.Class.method0(whatever:0)\nat package.Class.0method(whatever:1)\nat package.Class.method0method(whatever:2)");
  }

  @Test
  public void classWithNumbersInNameIsFound() throws Exception {
    stacktraceLogChecker.setExpectedCalls(asList(
                                                 new StacktraceLogChecker.MethodCall("package", "Class0", "method", 0),
                                                 new StacktraceLogChecker.MethodCall("package", "0Class", "method", 1),
                                                 new StacktraceLogChecker.MethodCall("package", "Class0class", "method", 2)));
    stacktraceLogChecker
        .check("at package.Class0.method(whatever:0)\nat package.0Class.method(whatever:1)\nat package.Class0class.method(whatever:2)");
  }



}
