/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
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
  private static final String PACKAGE = "package";
  private static final String CLASS = "Class";
  private static final String METHOD = "method";

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
    String log = format("this could be%s" +
        "the message part and\tshould not be evaluated%s" +
        "at package.Class.method(whatever:0)", lineSeparator(), lineSeparator());
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall(PACKAGE, CLASS, METHOD, 0);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check(log);
  }

  @Test
  public void callNotFoundShouldRaiseError() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall(PACKAGE, CLASS, METHOD, 0);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
    stacktraceLogChecker.check("this stacktrace does not contain expected call");
  }

  @Test
  public void callFoundShouldSucceed() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall(PACKAGE, CLASS, METHOD, 0);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:0)");
  }


  @Test
  public void noLineNumberSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall(PACKAGE, CLASS, METHOD);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void noPackageSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setMethod(METHOD);
    methodCall.setClazz(CLASS);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void noClassSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setMethod(METHOD);
    methodCall.setPackageName(PACKAGE);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void noMethodSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setClazz(CLASS);
    methodCall.setPackageName(PACKAGE);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void onlyMethodSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setMethod(METHOD);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void onlyClassSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setClazz(CLASS);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void onlyPackageSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setPackageName(PACKAGE);
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
    methodCall.setMethod(METHOD);
    methodCall.setPackageName(PACKAGE);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void methodAndClassSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setClazz(CLASS);
    methodCall.setMethod(METHOD);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void methodAndLineNumberSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setMethod(METHOD);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void packageAndClassSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setClazz(CLASS);
    methodCall.setPackageName(PACKAGE);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void packageAndLineNumberSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setPackageName(PACKAGE);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void classAndLineNumberSpecifiedShouldMatch() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall();
    methodCall.setLineNumber(25);
    methodCall.setClazz(CLASS);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void differentMethodRepresentsDifferentExpectedMethodCalls() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall(PACKAGE, CLASS, "otherMethod", 25);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void differentClassRepresentsDifferentExpectedMethodCalls() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall(PACKAGE, "OtherClass", METHOD, 25);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void differentPackageRepresentsDifferentExpectedMethodCalls() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall("other.package", CLASS, METHOD, 25);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void differentLineNumbersRepresentDifferentExpectedMethodCalls() throws Exception {
    StacktraceLogChecker.MethodCall methodCall = new StacktraceLogChecker.MethodCall(PACKAGE, CLASS, METHOD, 0);
    stacktraceLogChecker.setExpectedCalls(asList(methodCall));
    expectedException.expect(AssertionError.class);
    stacktraceLogChecker.check("at package.Class.method(whatever:25)");
  }

  @Test
  public void methodWithNumbersInNameIsFound() throws Exception {
    stacktraceLogChecker.setExpectedCalls(asList(
                                                 new StacktraceLogChecker.MethodCall(PACKAGE, CLASS, "method0", 0),
                                                 new StacktraceLogChecker.MethodCall(PACKAGE, CLASS, "0method", 1),
                                                 new StacktraceLogChecker.MethodCall(PACKAGE, CLASS, "method0method", 2)));
    stacktraceLogChecker
        .check(format("at package.Class.method0(whatever:0)%s" +
            "at package.Class.0method(whatever:1)%s" +
            "at package.Class.method0method(whatever:2)", lineSeparator(), lineSeparator()));
  }

  @Test
  public void classWithNumbersInNameIsFound() throws Exception {
    stacktraceLogChecker.setExpectedCalls(asList(
                                                 new StacktraceLogChecker.MethodCall(PACKAGE, "Class0", METHOD, 0),
                                                 new StacktraceLogChecker.MethodCall(PACKAGE, "0Class", METHOD, 1),
                                                 new StacktraceLogChecker.MethodCall(PACKAGE, "Class0class", METHOD, 2)));
    stacktraceLogChecker
        .check(format("at package.Class0.method(whatever:0)%s" +
            "at package.0Class.method(whatever:1)%s" +
            "at package.Class0class.method(whatever:2)", lineSeparator(), lineSeparator()));
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
        .check(format("noise, more noise, not %s" +
            "      important. \t%s" +
            "Still not important%s" +
            " Caused by: org.package.Exception%s" +
            " more irrelevant stuff", lineSeparator(), lineSeparator(), lineSeparator(), lineSeparator()));
  }



}
