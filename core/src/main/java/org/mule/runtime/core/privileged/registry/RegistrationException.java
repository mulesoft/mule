/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.registry;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

public final class RegistrationException extends MuleException {

  private static final long serialVersionUID = 9143114426140546639L;

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

