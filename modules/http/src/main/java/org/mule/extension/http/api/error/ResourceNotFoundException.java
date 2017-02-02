/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.error;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Thrown when a static file is requested but not found, associated with a 404 status code.
 *
 * @since 4.0
 */
public class ResourceNotFoundException extends ModuleException {

  private static final long serialVersionUID = 3137973689262542839L;

  public <T extends Enum<T>> ResourceNotFoundException(Exception exception,
                                                       ErrorTypeDefinition<T> errorTypeDefinition,
                                                       I18nMessage message) {
    super(exception, errorTypeDefinition, message);
  }

}
