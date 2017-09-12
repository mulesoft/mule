/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;


import static java.lang.System.lineSeparator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;

public class StacktraceLogChecker extends AbstractLogChecker {

  private List<MethodCall> expectedCalls = new ArrayList<>();

  @Override
  public void check(String logMessage) {
    StringBuilder errors = new StringBuilder();
    List<String> stackTraceLines = getStacktraceLinesFromLogLines(splitLines(logMessage));
    List<MethodCall> actualStackCalls = new ArrayList<>();
    for (String line : stackTraceLines) {
      actualStackCalls.add(createMethodCallFromLine(line));
    }
    for (MethodCall call : expectedCalls) {
      if (!actualStackCalls.contains(call)) {
        errors.append(lineSeparator());
        errors.append(String.format("Expected method call: %s was not in stack trace", call.toString()));
        errors.append(lineSeparator());
      }
    }

    String errorMessage = errors.toString();
    if (!StringUtils.isBlank(errorMessage)) {
      throw new AssertionError(errorMessage);
    }
  }


  private MethodCall createMethodCallFromLine(String line) {
    Matcher matcher = STACKTRACE_METHOD_CALL_REGEX_PATTERN.matcher(line);
    if (matcher.matches()) {
      //If no line number found, probably due to native method
      if (matcher.group(4).equals(StringUtils.EMPTY)) {
        return new MethodCall(matcher.group(1), matcher.group(2), matcher.group(3));
      }
      return new MethodCall(matcher.group(1), matcher.group(2), matcher.group(3), Integer.parseInt(matcher.group(4)));
    }
    return new MethodCall();
  }

  public void setExpectedCalls(List<MethodCall> expectedCalls) {
    this.expectedCalls = expectedCalls;
  }

  public List<MethodCall> getExpectedCalls() {
    return this.expectedCalls;
  }

  public static class MethodCall {

    private String packageName = null;
    private String clazz = null;
    private String method = null;
    Integer lineNumber = null;

    private void setFields(String packageName, String clazz, String method) {
      this.packageName = packageName;
      this.clazz = clazz;
      this.method = method;
    }

    public MethodCall() {}

    public MethodCall(String packageName, String clazz, String method) {
      setFields(packageName, clazz, method);
    }

    public MethodCall(String packageName, String clazz, String method, Integer lineNumber) {
      setFields(packageName, clazz, method);
      this.lineNumber = lineNumber;
    }

    public boolean isLineNumberSet() {
      return this.lineNumber != null;
    }

    public String getPackageName() {
      return packageName;
    }

    public void setPackageName(String packageName) {
      this.packageName = packageName;
    }

    public String getClazz() {
      return clazz;
    }

    public void setClazz(String clazz) {
      this.clazz = clazz;
    }

    public String getMethod() {
      return method;
    }

    public void setMethod(String method) {
      this.method = method;
    }

    public Integer getLineNumber() {
      return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
      this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
      return String.format("%s.%s.%s:%d", packageName, clazz, method, lineNumber);
    }

    @Override
    public int hashCode() {
      int methodHashCode = method != null ? method.hashCode() : 7;
      int packageHashCode = packageName != null ? packageName.hashCode() : 7;
      int classHashCode = clazz != null ? clazz.hashCode() : 7;
      int lineHashCode = lineNumber != null ? lineNumber.hashCode() : 7;
      return methodHashCode + packageHashCode + classHashCode + lineHashCode;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof MethodCall)) {
        return false;
      }
      if (this == obj) {
        return true;
      }
      if (method != null && !method.equals(((MethodCall) obj).method)) {
        return false;
      }
      if (clazz != null && !clazz.equals(((MethodCall) obj).clazz)) {
        return false;
      }
      if (packageName != null && !packageName.equals(((MethodCall) obj).packageName)) {
        return false;
      }
      if (lineNumber != null && !lineNumber.equals(((MethodCall) obj).lineNumber)) {
        return false;
      }
      return true;
    }
  }

}
