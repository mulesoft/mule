/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import org.mule.sdk.api.runtime.exception.ExceptionHandler;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.runtime.exception.ExceptionHandler} into an sdk-api
 * {@link ExceptionHandler}
 *
 * @since 4.5.0
 */
public class SdkExceptionHandlerAdapter extends ExceptionHandler {

  public SdkExceptionHandlerAdapter(org.mule.runtime.extension.api.runtime.exception.ExceptionHandler handler) {

  }

  public SdkExceptionHandlerAdapter(ExceptionHandler handler) {

  }

  @Override
  public Exception enrichException(Exception e) {
    return null;
  }
}
