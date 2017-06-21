/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.bootstrap;

import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Represents exceptions during the bootstrap configuration process
 */
public class BootstrapException extends ConfigurationException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 3658223240493754960L;

  public BootstrapException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public BootstrapException(I18nMessage message) {
    super(message);
  }
}
