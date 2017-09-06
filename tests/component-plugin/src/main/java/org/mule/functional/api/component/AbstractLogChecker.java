/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;


import static java.lang.System.lineSeparator;
import static org.mule.runtime.api.exception.MuleException.EXCEPTION_MESSAGE_SECTION_DELIMITER;
import static org.mule.runtime.core.api.util.StringUtils.EMPTY;

public abstract class AbstractLogChecker implements LogChecker {

  @Override
  public abstract void check(String logMessage);

  /**
   * Extracts the message part of the log. Separating it from the stack trace.
   *
   * @return a string with the log message
   */
  protected String extractMessageFromLog(String initialLog) {
    return initialLog.split(EXCEPTION_MESSAGE_SECTION_DELIMITER)[0];
  }

  /**
   * Extracts the stack trace from the log, ignoring any other information that could be there.
   *
   * @return a string with the log stack trace.
   */
  protected String extractStacktraceFromLog(String initialLog) {
    String[] splittedLog = initialLog.split(EXCEPTION_MESSAGE_SECTION_DELIMITER);
    if(splittedLog.length < 2) {
      return EMPTY;
    }
    return initialLog.split(EXCEPTION_MESSAGE_SECTION_DELIMITER)[1];
  }


  protected String[] splitLines(String wholeMessage) {
    return wholeMessage.split(lineSeparator());
  }

}
