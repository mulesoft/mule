package org.mule.functional.api.component;


public class EqualsLogChecker implements LogChecker{

  private String expectedLogMessage;
  private boolean shouldFilterLogMessage;


  @Override
  public void check(String logMessage) {
    //Do nothing
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
