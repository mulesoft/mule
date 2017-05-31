/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.security.ServerSecurityException;

/**
 * <code>EncryptionNotSupportedException</code> is thrown if an algorithm is set in the MULE_USER header but it doesn't match the
 * algorithm set on the security filter
 */

public class EncryptionNotSupportedException extends ServerSecurityException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -1661059380853528624L;

  public EncryptionNotSupportedException(I18nMessage message) {
    super(message);
  }

  public EncryptionNotSupportedException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
