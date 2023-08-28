/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.config;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

public class ConfigurationException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 3658822340943734962L;

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
