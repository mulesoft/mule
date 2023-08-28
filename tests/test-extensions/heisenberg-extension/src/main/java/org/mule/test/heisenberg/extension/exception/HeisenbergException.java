/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.exception;

public class HeisenbergException extends Exception {

  public HeisenbergException(String message) {
    super(message);
  }

  public HeisenbergException(String message, Throwable cause) {
    super(message, cause);
  }
}
