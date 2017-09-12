/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.lang.System.lineSeparator;
import static org.mule.runtime.api.exception.MuleException.EXCEPTION_MESSAGE_DELIMITER;
import static org.mule.runtime.core.api.util.StringUtils.EMPTY;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class EqualsLogChecker extends AbstractLogChecker {

  private String expectedLogMessage = EMPTY;
  private boolean shouldFilterLogMessage;


  @Override
  public void check(String logMessage) {
    StringBuilder errors = new StringBuilder();
    List<String> expectedLines = splitLines(expectedLogMessage);
    List<String> actualLines = getMessageLinesFromLogLines(splitLines(logMessage));

    checkLineCount(expectedLines, actualLines, errors);

    compareLines(expectedLines, actualLines, errors);

    String errorMessage = errors.toString();
    if (!StringUtils.isBlank(errorMessage)) {
      throw new AssertionError(errorMessage);
    }
  }

  private void checkLineCount(List<String> expectedLog, List<String> actualLog, StringBuilder errorCatcher) {
    if (expectedLog.size() != actualLog.size()) {
      errorCatcher.append(lineSeparator());
      errorCatcher.append(String.format("Log lines differs from expected one. It has %d lines and it's expecting %d\n",
                                        actualLog.size(), expectedLog.size()));
      errorCatcher.append(lineSeparator());
    }
  }

  private void compareLines(List<String> expectedLogLines, List<String> actualLogLines, StringBuilder errorCatcher) {
    int i;
    for (i = 0; i < expectedLogLines.size(); i++) {
      if (i >= actualLogLines.size()) {
        errorCatcher.append(String.format("Missing expected line[%d]: %s\n", i, expectedLogLines.get(i)));
      } else {
        if (!(expectedLogLines.get(i).trim().equals(actualLogLines.get(i).trim()))) {
          errorCatcher.append(String.format("Difference found in line %d: \nEXPECTED: %s\nFOUND: %s\n", i,
                                            expectedLogLines.get(i).trim(), actualLogLines.get(i).trim()));
          errorCatcher.append(lineSeparator());
        }
      }
    }
    if (actualLogLines.size() > expectedLogLines.size()) {
      errorCatcher.append("Actual log has extra lines:\n");
      for (int j = i; j < actualLogLines.size(); j++) {
        errorCatcher.append(actualLogLines.get(j));
        errorCatcher.append(lineSeparator());
      }
      errorCatcher.append(lineSeparator());
    }
  }


  @Override
  protected List<String> splitLines(String wholeMessage) {
    if (shouldFilterLogMessage) {
      return filterLines(super.splitLines(wholeMessage));
    }
    return super.splitLines(wholeMessage);
  }

  private List<String> filterLines(List<String> splittedLog) {
    return splittedLog.stream()
        .filter((line) -> StringUtils.isNotBlank(line) && !line.trim().equals(EXCEPTION_MESSAGE_DELIMITER.trim()))
        .collect(Collectors.toList());
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
