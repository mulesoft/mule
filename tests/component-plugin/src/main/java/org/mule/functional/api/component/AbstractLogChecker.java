package org.mule.functional.api.component;


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
  protected String getLogMessage(String initialLog) {
    return initialLog.split(EXCEPTION_MESSAGE_SECTION_DELIMITER)[0];
  }

  /**
   * Extracts the stack trace from the log, ignoring any other information that could be there.
   *
   * @return a string with the log stack trace.
   */
  protected String getStackTrace(String initialLog) {
    String[] splittedLog = initialLog.split(EXCEPTION_MESSAGE_SECTION_DELIMITER);
    if(splittedLog.length < 2) {
      return EMPTY;
    }
    return initialLog.split(EXCEPTION_MESSAGE_SECTION_DELIMITER)[1];
  }

}
