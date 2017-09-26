/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.registry;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

public class RegistrationException extends MuleException {

  private static final long serialVersionUID = 9143114426140546637L;

  public RegistrationException(I18nMessage message) {
    super(message);
  }

  public RegistrationException(Throwable cause) {
    super(cause);
  }

  public RegistrationException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

}

