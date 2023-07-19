/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.api.i18n.I18nMessage;

public final class TransactionRollbackException extends TransactionException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -3247455113055556219L;

  /**
   * @param message the exception message
   */
  public TransactionRollbackException(I18nMessage message) {
    super(message);
  }

  /**
   * @param message the exception message
   * @param cause   the exception that cause this exception to be thrown
   */
  public TransactionRollbackException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public TransactionRollbackException(Throwable cause) {
    super(CoreMessages.transactionRollbackFailed(), cause);
  }

}
