/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import org.mule.runtime.api.exception.MuleRuntimeException;

/**
 * Signals that the SDK invoked an extension's method which threw an exception. The exception thrown will be available as the
 * cause.
 * <p>
 * Because this is a {@link MuleRuntimeException}, it will not automatically fill the stack trace, providing a performance boost.
 *
 * @since 4.3.0
 */
public class SdkMethodInvocationException extends MuleRuntimeException {

  private static final long serialVersionUID = 8708990792854682118L;

  public SdkMethodInvocationException(Throwable cause) {
    super(cause);
  }
}
