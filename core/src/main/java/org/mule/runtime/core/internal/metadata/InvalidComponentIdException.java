/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.core.api.MuleContext;

/**
 * Represents that the given {@link Location} is invalid due that the Component could not be found in the current
 * {@link MuleContext}.
 *
 * @since 4.0
 */
public class InvalidComponentIdException extends MuleException {

  private final FailureCode failureCode;

  /**
   * @param message the exception message
   */
  InvalidComponentIdException(I18nMessage message) {
    super(message);
    failureCode = UNKNOWN;
  }

  /**
   * @param message the exception message
   * @param code {@link FailureCode} associated to the exception
   */
  InvalidComponentIdException(I18nMessage message, FailureCode code) {
    super(message);
    failureCode = code;
  }

  /**
   * @param message the exception message
   * @param cause the exception that cause this exception to be thrown
   */
  InvalidComponentIdException(I18nMessage message, Throwable cause) {
    super(message, cause);
    failureCode = UNKNOWN;
  }

  /**
   * @param message the exception message
   * @param cause the exception that cause this exception to be thrown
   * @param code {@link FailureCode} associated to the exception
   */
  InvalidComponentIdException(I18nMessage message, Throwable cause, FailureCode code) {
    super(message, cause);
    failureCode = code;
  }

  public FailureCode getFailureCode() {
    return failureCode;
  }
}
