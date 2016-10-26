/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * <code>ManagementException</code> is a general exception thrown by management extensions.
 */
public abstract class ManagementException extends MuleException {

  /**
   * @param message the exception message
   */
  protected ManagementException(I18nMessage message) {
    super(message);
  }

  /**
   * @param message the exception message
   * @param cause the exception that cause this exception to be thrown
   */
  protected ManagementException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  protected ManagementException(Throwable cause) {
    super(cause);
  }
}
