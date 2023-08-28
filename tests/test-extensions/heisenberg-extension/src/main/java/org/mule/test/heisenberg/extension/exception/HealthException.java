/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.exception;

public class HealthException extends Exception {

  public HealthException(String message) {
    super(message);
  }
}
