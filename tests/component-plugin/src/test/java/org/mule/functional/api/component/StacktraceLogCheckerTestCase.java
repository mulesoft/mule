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

import java.io.PrintWriter;
import java.io.StringWriter;

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
  public void noPackageSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setMethod("method");
    methodCall.setClazz("Class");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void noClassSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setMethod("method");
    methodCall.setPackageName("package");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void noMethodSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setClazz("Class");
    methodCall.setPackageName("package");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void onlyMethodSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setMethod("method");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void onlyClassSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setClazz("Class");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void onlyPackageSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setPackageName("package");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void onlyLineSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void methodAndPackageSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setMethod("method");
    methodCall.setPackageName("package");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void methodAndClassSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setClazz("Class");
    methodCall.setMethod("method");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void methodAndLineNumberSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setMethod("method");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void packageAndClassSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setClazz("Class");
    methodCall.setPackageName("package");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void packageAndLineNumberSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setPackageName("package");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void classAndLineNumberSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setClazz("Class");
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void differentMethodRepresentsDifferentExpectedMethodCalls() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("package", "Class", "otherMethod", 25);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void differentClassRepresentsDifferentExpectedMethodCalls() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("package", "OtherClass", "method", 25);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void differentPackageRepresentsDifferentExpectedMethodCalls() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("other.package", "Class", "method", 25);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
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

  @Test
  public void evaluatesRealStacktrace() throws Exception {
    stacktraceLogChecker
        .setExpectedCalls(asList(new StacktraceLogChecker.MethodCall("org.mule.functional.api.component",
                                                                     "StacktraceLogCheckerTestCase", "evaluatesRealStacktrace")));
    StringWriter s = new StringWriter();
    PrintWriter p = new PrintWriter(s);
    new Exception().printStackTrace(p);
    stacktraceLogChecker.check(s.toString());
  }

  @Test
  public void causeMatchSuccess() throws Exception {
    stacktraceLogChecker.setExpectedExceptionCauses(asList(new StacktraceLogChecker.ExceptionCause("org.package.Exception")));
    stacktraceLogChecker
        .check("noise, more noise, not \n      important. \t\nStill not important\n Caused by: org.package.Exception\n more irrelevant stuff");
  }



}
