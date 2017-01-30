/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleException;

/**
 * {@code EncryptionStrategyNotFoundException} thrown by the {@link java.lang.SecurityManager} when an encryption
 * scheme is set in a property or header that has not been registered with the manager.
 *
 * @since 4.0
 */
public class EncryptionStrategyNotFoundException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 3916371211189075139L;

  public EncryptionStrategyNotFoundException(String strategyName) {
    super(createStaticMessage("There is no Encryption Strategy registered called '%s'", strategyName));
  }

  public EncryptionStrategyNotFoundException(String strategyName, Throwable cause) {
    super(createStaticMessage("There is no Encryption Strategy registered called '%s'", strategyName), cause);
  }
}
