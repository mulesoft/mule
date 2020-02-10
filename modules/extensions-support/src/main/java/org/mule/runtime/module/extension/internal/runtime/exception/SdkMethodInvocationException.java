/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import org.mule.runtime.api.exception.MuleRuntimeException;

/**
 * Signals that the SDK invoked an extension's method which threw an exception. The exception thrown will be available as
 * the cause.
 * <p>
 * Because this is a {@link MuleRuntimeException}, it will not automatically fill the stack trace, providing a performance
 * boost.
 *
 * @since 4.3.0
 */
public class SdkMethodInvocationException extends MuleRuntimeException {

  public SdkMethodInvocationException(Throwable cause) {
    super(cause);
  }
}
