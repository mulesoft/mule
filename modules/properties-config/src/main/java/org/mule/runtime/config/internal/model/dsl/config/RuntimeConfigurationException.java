/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.model.dsl.config;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Configuration exception thrown due to a invalid configuration settings
 */
public class RuntimeConfigurationException extends MuleRuntimeException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 3658822340943734963L;

  public RuntimeConfigurationException(I18nMessage message) {
    super(message);
  }

  public RuntimeConfigurationException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
