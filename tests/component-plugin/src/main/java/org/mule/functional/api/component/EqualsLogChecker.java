/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mule.runtime.api.exception.MuleException.EXCEPTION_MESSAGE_DELIMITER;
import static org.mule.runtime.core.api.util.StringUtils.EMPTY;

import java.util.List;


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
    if (isNotBlank(errorMessage)) {
      throw new AssertionError(lineSeparator() + errorMessage);
    }
  }

  private void checkLineCount(List<String> expectedLog, List<String> actualLog, StringBuilder errorCatcher) {
    assertAndSaveError(actualLog.size(),
                       is(equalTo(expectedLog.size())),
                       "Log lines differs from expected ones:",
                       errorCatcher);
  }

  private void compareLines(List<String> expectedLogLines, List<String> actualLogLines, StringBuilder errorCatcher) {
    int i;
    for (i = 0; i < expectedLogLines.size(); i++) {
      if (i >= actualLogLines.size()) {
        errorCatcher
            .append(format("%sMissing expected line[%d]: %s%s", lineSeparator(), i, expectedLogLines.get(i), lineSeparator()));
      } else {
        assertAndSaveError(actualLogLines.get(i),
                           is(equalToIgnoringWhiteSpace(expectedLogLines.get(i))),
                           format("Difference found in line %d:", i),
                           errorCatcher);
      }
    }
    if (actualLogLines.size() > expectedLogLines.size()) {
      errorCatcher.append(lineSeparator());
      errorCatcher.append("Actual log has extra lines:");
      errorCatcher.append(lineSeparator());
      for (int j = i; j < actualLogLines.size(); j++) {
        errorCatcher.append(actualLogLines.get(j));
        errorCatcher.append(lineSeparator());
      }
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
        .filter((line) -> isNotBlank(line) && !line.trim().equals(EXCEPTION_MESSAGE_DELIMITER.trim()))
        .collect(toList());
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
