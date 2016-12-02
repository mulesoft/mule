/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.exception;

/**
 * Base {@link RuntimeException} type for the Web Service Consumer.
 *
 * @since 4.0
 */
public class WscException extends RuntimeException {

  public WscException(String message, Throwable cause) {
    super(message, cause);
  }
}
