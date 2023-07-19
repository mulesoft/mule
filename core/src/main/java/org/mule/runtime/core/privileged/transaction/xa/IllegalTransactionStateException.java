/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.transaction.xa;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.core.api.transaction.TransactionStatusException;

/**
 * <code>IllegalTransactionStateException</code> TODO (document class)
 */
public final class IllegalTransactionStateException extends TransactionStatusException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -1062247038945694387L;

  /**
   * @param message the exception message
   */
  public IllegalTransactionStateException(I18nMessage message) {
    super(message);
  }

  /**
   * @param message the exception message
   * @param cause   the exception that cause this exception to be thrown
   */
  public IllegalTransactionStateException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

}
