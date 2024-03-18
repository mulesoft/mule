/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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