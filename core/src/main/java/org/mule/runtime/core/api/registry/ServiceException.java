/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.registry;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Any service-related exception: service not found, service lookup error, etc.
 */
public class ServiceException extends MuleException {

  public ServiceException(I18nMessage message) {
    super(message);
  }

  public ServiceException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
