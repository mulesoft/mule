/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.api.i18n.I18nMessage;

public class TransactionStatusException extends TransactionException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -2408368544426562866L;

  /**
   * @param message the exception message
   */
  public TransactionStatusException(I18nMessage message) {
    super(message);
  }

  /**
   * @param message the exception message
   * @param cause   the exception that cause this exception to be thrown
   */
  public TransactionStatusException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public TransactionStatusException(Throwable cause) {
    super(CoreMessages.transactionCannotReadState(), cause);
  }
}
