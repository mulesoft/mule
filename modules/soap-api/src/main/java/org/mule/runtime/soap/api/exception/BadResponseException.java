/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.soap.api.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;

/**
 * {@link Exception} implementation that aims to be thrown when an error occur while parsing or processing the SOAP response.
 *
 * @since 4.0
 */
public final class BadResponseException extends MuleRuntimeException {

  public BadResponseException(String message) {
    super(createStaticMessage(message));
  }

  public BadResponseException(String message, Throwable cause) {
    super(createStaticMessage(message), cause);
  }
}
