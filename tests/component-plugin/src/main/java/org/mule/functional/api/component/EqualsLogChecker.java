package org.mule.functional.api.component;

import static java.lang.System.lineSeparator;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.exception.MuleException.EXCEPTION_MESSAGE_DELIMITER;

import java.util.Arrays;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

public class EqualsLogChecker extends AbstractLogChecker {

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

    String errorMessage = errors.toString();
    if(!StringUtils.isBlank(errorMessage)) {
      fail(errors.toString());
    }

  }

  private void checkLineCount(String[] expectedLog, String[] actualLog, StringBuilder errorCatcher) {
    if(expectedLog.length != actualLog.length) {
      errorCatcher.append(lineSeparator());
      errorCatcher.append(String.format("Log lines differs from expected one. It has %d lines and it's expecting %d\n",actualLog.length,expectedLog.length));
      errorCatcher.append(lineSeparator());
    }
  }

  private void compareLines(String[] expectedLogLines, String[] actualLogLines, StringBuilder errorCatcher) {
    int i;
    for(i = 0; i < expectedLogLines.length ; i++) {
      if(i >= actualLogLines.length) {
        errorCatcher.append(String.format("Missing expected line[%d]: %s\n",i,expectedLogLines[i]));
      }else {
        if(!(expectedLogLines[i].trim().equals(actualLogLines[i].trim()))) {
          errorCatcher.append(String.format("Difference found in line %d: \nEXPECTED: %s\nFOUND: %s\n",i,expectedLogLines[i].trim(), actualLogLines[i].trim()));
          errorCatcher.append(lineSeparator());
        }
      }
    }
    if(actualLogLines.length > expectedLogLines.length) {
      errorCatcher.append("Actual log has extra lines:\n");
      for(int j = i;j < actualLogLines.length; j++) {
        errorCatcher.append(actualLogLines[j]);
        errorCatcher.append(lineSeparator());
      }
      errorCatcher.append(lineSeparator());
    }
  }

  private String[] splitLines(String wholeMessage) {
    if(shouldFilterLogMessage){
      return splitLines(wholeMessage, (line) -> StringUtils.isNotBlank(line) && !line.trim().equals(EXCEPTION_MESSAGE_DELIMITER.trim()));
    }
    return splitLines(wholeMessage,(line) -> true);
  }

  private String[] splitLines(String wholeMessage, Predicate<String> filter) {
    return Arrays.stream(wholeMessage.split(lineSeparator())).filter(filter).toArray(String[]::new);
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
