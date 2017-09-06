/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static org.mule.runtime.api.exception.MuleException.EXCEPTION_MESSAGE_SECTION_DELIMITER;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractLogChecker implements LogChecker {

  protected static final Pattern PARSING_REGEX_PATTERN = Pattern.compile("^.*at ([^A-Z]*)\\.([a-zA-Z]*)\\.([^\\(]*)[^:]*:([0-9]*).*");

  @Override
  public abstract void check(String logMessage);

  /**
   * Extracts the message part of the log. Separating it from the stack trace.
   *
   * @return a string list with the log message lines
   */
  protected List<String> getMessageLinesFromLogLines(List<String> initialLogLines) {
    int index = initialLogLines.indexOf(EXCEPTION_MESSAGE_SECTION_DELIMITER.trim());
    if(index > -1) {
      return initialLogLines.subList(0,index);
    }else {
      return removeStacktraceLines(initialLogLines);
    }
  }

  /**
   * Extracts the stack trace from the log, ignoring any other information that could be there.
   *
   * @return a string list with the log stack trace lines.
   */
  protected List<String> getStacktraceLinesFromLogLines(List<String> initialLogLines) {
    int index = initialLogLines.indexOf(EXCEPTION_MESSAGE_SECTION_DELIMITER.trim());
    if(index > -1) {
      return initialLogLines.subList(index, initialLogLines.size());
    }else {
      return getStacktraceLines(initialLogLines);
    }
  }

  private List<String> getStacktraceLines(List<String> allLines) {
    return allLines.stream().filter((line) -> PARSING_REGEX_PATTERN.matcher(line).matches()).collect(Collectors.toList());
  }

  private List<String> removeStacktraceLines(List<String> allLines) {
    return allLines.stream().filter((line) -> ! PARSING_REGEX_PATTERN.matcher(line).matches()).collect(Collectors.toList());
  }

  protected List<String> splitLines(String wholeMessage) {
    return asList(wholeMessage.split(lineSeparator()));
  }

}
