/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.soap.api.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;

/**
 * {@link Exception} implementation that aims to be thrown when an inconsistent WSDL state is found or a provided one cannot be
 * parsed.
 *
 * @since 4.0
 */
public final class InvalidWsdlException extends MuleRuntimeException {

  public InvalidWsdlException(String message) {
    super(createStaticMessage(message));
  }

  public InvalidWsdlException(String message, Throwable cause) {
    super(createStaticMessage(message), cause);
  }
}
