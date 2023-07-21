/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.exception;

/**
 * Exceptions to be thrown when a received status code was not between the expected range
 *
 * @since 4.2.0
 */
public class InvalidStatusCodeException extends Exception {

  private final int status;

  /**
   * Creates a new instance
   *
   * @param status the received status code
   */
  public InvalidStatusCodeException(int status) {
    super("Invalid Status Code " + status);
    this.status = status;
  }

  /**
   * @return the received status code
   */
  public int getStatus() {
    return status;
  }
}
