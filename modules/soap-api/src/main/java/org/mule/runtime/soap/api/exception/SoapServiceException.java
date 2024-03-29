/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.exception;

/**
 * Base {@link RuntimeException} type for the Web Service Consumer.
 *
 * @since 4.0
 */
public final class SoapServiceException extends RuntimeException {

  public SoapServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
