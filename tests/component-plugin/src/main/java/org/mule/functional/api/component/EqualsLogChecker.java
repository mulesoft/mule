package org.mule.functional.api.component;

import org.mule.runtime.core.api.DefaultMuleException;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

public class EqualsLogChecker implements LogChecker{

  private static final String LINE_JUMP = "\n";

  private String expectedLogMessage;
  private boolean shouldFilterLogMessage;


  @Override
  public void check(String logMessage) {
    if(shouldFilterLogMessage) {
      logMessage = filterLogMessage(logMessage);
    }
    StringBuilder errors = new StringBuilder();
    String[] expectedLines = splitLines(expectedLogMessage);
    String[] actualLines = splitLines(logMessage);

    checkLineCount(expectedLines, actualLines, errors);

    compareLines(expectedLines, actualLines, errors);

    System.out.println(errors.toString());
  }

  private void checkLineCount(String[] expectedLog, String[] actualLog, StringBuilder errorCatcher) {
    if(expectedLog.length != actualLog.length) {
      errorCatcher.append(String.format("Log lines differs from expected one. It has %d lines and it's expecting %d\n",actualLog.length,expectedLog.length));
      errorCatcher.append(LINE_JUMP);
    }
  }

  private void compareLines(String[] expectedLogLines, String[] actualLogLines, StringBuilder errorCatcher) {
    int i = 0;
    for(i = 0; i < expectedLogLines.length ; i++) {
      if(i >= actualLogLines.length) {
        errorCatcher.append(String.format("Missing expected line[%d]: %s\n",i,expectedLogLines[i]));
      }else {
        if(!(expectedLogLines[i].trim().equals(actualLogLines[i].trim()))) {
          errorCatcher.append(String.format("Difference found in line %d: \nEXPECTED: %s\nACTUAL: %s\n",i,expectedLogLines[i].trim(), actualLogLines[i].trim()));
          errorCatcher.append(LINE_JUMP);
        }
      }
    }
    if(actualLogLines.length > expectedLogLines.length) {
      errorCatcher.append("Actual log has extra lines:\n");
      for(int j = i;j < actualLogLines.length; j++) {
        errorCatcher.append(actualLogLines[j]);
        errorCatcher.append(LINE_JUMP);
      }
    }
  }

  private String[] splitLines(String wholeMessage) {
    return Arrays.stream(wholeMessage.split(LINE_JUMP)).filter(StringUtils::isNotBlank).toArray(String[]::new);
  }

  private String filterLogMessage(String logMessage) {
    return logMessage;
  }

  public void setExpectedLogMessage(String expectedLogMessage) {
    this.expectedLogMessage = expectedLogMessage;
  }

  public String getExpectedLogMessage() {
    return this.expectedLogMessage;
  }

  public void setShouldFilterLogMessage(boolean shouldFilterLogMessage) {
    this.shouldFilterLogMessage = shouldFilterLogMessage;
  }

  public boolean getShouldFilterLogMessage() {
    return this.shouldFilterLogMessage;
  }

}
