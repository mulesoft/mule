/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.exception;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Exception thrown when there's a failure writing the response using the transport infrastructure.
 */
@NoExtend
public class ResponseDispatchException extends MuleException {

  public ResponseDispatchException(I18nMessage message) {
    super(message);
  }

  public ResponseDispatchException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public ResponseDispatchException(Throwable cause) {
    super(cause);
  }

}
