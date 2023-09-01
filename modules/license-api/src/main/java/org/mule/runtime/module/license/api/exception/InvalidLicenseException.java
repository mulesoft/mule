/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
