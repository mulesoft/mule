/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

public class ConfigurationException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 3658822340943734960L;

  public ConfigurationException(I18nMessage message) {
    super(message);
  }

  public ConfigurationException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public ConfigurationException(Throwable cause) {
    super(cause);
  }
}
