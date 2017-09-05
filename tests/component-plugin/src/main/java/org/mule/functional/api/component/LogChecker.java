package org.mule.functional.api.component;

public interface LogChecker {

  /**
   * The method should check the log according to the rules defined in the implementation.
   *
   * @param logMessage the log message to check
   *
   * @throws Exception if the check fails
   */
  public void check(String logMessage);

}
