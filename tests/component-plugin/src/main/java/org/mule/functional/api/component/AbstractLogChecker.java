package org.mule.functional.api.component;


public class AbstractLogChecker implements LogChecker {

  @Override
  public void check(String logMessage) {
    //Do nothing
  }

  /**
   * Extracts the message part of the log. Separating it from the stack trace.
   *
   * @return a string with the log message
   */
  protected String getLogMessage() {
    return null;
  }

  /**
   * Extracts the stack trace from the log, ignoring any other information that could be there.
   *
   * @return a string with the log stack trace.
   */
  protected String getStackTrace() {
    return null;
  }

}
