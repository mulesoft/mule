/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Exception to be thrown when a resource could not be found within an artifact.
 *
 * @since 4.2
 */
public class ResourceNotFoundException extends MuleRuntimeException {

  public ResourceNotFoundException(I18nMessage message) {
    super(message);
  }

  public ResourceNotFoundException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
