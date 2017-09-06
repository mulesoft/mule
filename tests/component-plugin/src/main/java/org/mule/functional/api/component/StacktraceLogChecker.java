package org.mule.functional.api.component;


import static java.lang.System.lineSeparator;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.util.StringUtils.EMPTY;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class StacktraceLogChecker extends AbstractLogChecker{

  private static final Pattern PARSING_REGEX_PATTERN = Pattern.compile("^.*at ([^A-Z]*)\\.([a-zA-Z]*)\\.([^\\(]*)[^:]*:([0-9]*).*");

  private List<MethodCall> expectedCalls;

  @Override
  public void check(String logMessage) {
    StringBuilder errors = new StringBuilder();
    String[] stackTraceLines = splitLines(extractStacktraceFromLog(logMessage));
    Set<MethodCall> actualStackCalls = new HashSet<>();
    for(String line : stackTraceLines) {
      actualStackCalls.add(createMethodCallFromLine(line));
    }
    for(MethodCall call: expectedCalls) {
      if(!actualStackCalls.contains(call)){
        errors.append(lineSeparator());
        errors.append(String.format("Expected method call: %s was not in stack trace", call.toString()));
        errors.append(lineSeparator());
      }
    }

    String errorMessage = errors.toString();
    if(!StringUtils.isBlank(errorMessage)) {
      fail(errors.toString());
    }
  }

  private MethodCall createMethodCallFromLine(String line) {
    Matcher matcher = PARSING_REGEX_PATTERN.matcher(line);
    if(matcher.matches()) {
      return new MethodCall(matcher.group(1),matcher.group(2),matcher.group(3),Integer.parseInt(matcher.group(4)));
    }
    return new MethodCall();
  }

  public void setExpectedCalls(List<MethodCall> expectedCalls)  {
    this.expectedCalls = expectedCalls;
  }

  public List<MethodCall> getExpectedCalls() {
    return this.expectedCalls;
  }

  public static class MethodCall {

    private String packageName = EMPTY;
    private String clazz = EMPTY;
    private String method = EMPTY;
    Integer lineNumber = null;

    private void setFields(String packageName,String clazz,String method) {
      this.packageName = packageName;
      this.clazz = clazz;
      this.method = method;
    }

    public MethodCall() {}

    public MethodCall(String packageName,String clazz,String method) {
      setFields(packageName,clazz,method);
    }

    public MethodCall(String packageName,String clazz,String method,Integer lineNumber) {
      setFields(packageName,clazz,method);
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
      return String.format("%s.%s.%s:%d",packageName,clazz,method,lineNumber);
    }

    @Override
    public int hashCode() {
      int lineNumberHash = 7;
      if(lineNumber != null) {
        lineNumberHash = lineNumber.hashCode();
      }
      return this.packageName.hashCode() + this.clazz.hashCode() + this.method.hashCode() + lineNumberHash;
    }

    @Override
    public boolean equals(Object obj) {
      if(!(obj instanceof MethodCall)) {
        return false;
      }
      if(this == obj){
        return true;
      }
      if(this.lineNumber != null && ((MethodCall) obj).lineNumber != null) {
        if(!this.lineNumber.equals(((MethodCall) obj).lineNumber)) {
          return false;
        }
      }
      return packageName.equals(((MethodCall) obj).packageName) && clazz.equals(((MethodCall) obj).clazz) && method.equals(((MethodCall) obj).method);
    }
  }

}
