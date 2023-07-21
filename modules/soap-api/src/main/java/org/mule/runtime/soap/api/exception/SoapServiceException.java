/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
