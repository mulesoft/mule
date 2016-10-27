/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

/**
 * Generic exception for when the resolution of a value fails
 * 
 * @since 4.0
 */
public class ValueResolvingException extends Exception {

  public ValueResolvingException() {
    super();
  }

  public ValueResolvingException(String message) {
    super(message);
  }

  public ValueResolvingException(String message, Throwable cause) {
    super(message, cause);
  }
}
