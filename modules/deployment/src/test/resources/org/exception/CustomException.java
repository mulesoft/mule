/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.exception;

import org.mule.runtime.api.exception.MuleException;

public class CustomException extends MuleException {

  private static final String MESSAGE = "Error";

  @Override
  public String getDetailedMessage() {
    return MESSAGE;
  }

  @Override
  public String getVerboseMessage() {
    return MESSAGE;
  }

  @Override
  public String getSummaryMessage() {
    return MESSAGE;
  }
}