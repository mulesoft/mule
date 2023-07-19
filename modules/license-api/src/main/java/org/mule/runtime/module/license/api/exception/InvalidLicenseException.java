/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.license.api.exception;

public class InvalidLicenseException extends RuntimeException {


  public InvalidLicenseException(String message) {
    super(message);
  }

  public InvalidLicenseException(String message, Exception cause) {
    super(message, cause);
  }
}
