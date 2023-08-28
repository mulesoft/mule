/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * <code>MuleContextException</code> is thrown when an exception occurs with Mule Context objects
 */
public final class MuleContextException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -1526680893293714178L;

  /**
   * @param message the exception message
   */
  public MuleContextException(I18nMessage message) {
    super(message);
  }

  /**
   * @param message the exception message
   * @param cause   the exception that cause this exception to be thrown
   */
  public MuleContextException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
