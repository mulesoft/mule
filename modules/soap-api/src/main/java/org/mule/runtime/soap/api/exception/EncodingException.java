/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.soap.api.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;

/**
 * {@link Exception} implementation that aims to be thrown when a problem occur while encoding or decoding the content of a part
 * of a SOAP message.
 *
 * @since 4.0
 */
public final class EncodingException extends MuleRuntimeException {

  public EncodingException(String message) {
    super(createStaticMessage(message));
  }

  public EncodingException(String message, Throwable cause) {
    super(createStaticMessage(message), cause);
  }
}
