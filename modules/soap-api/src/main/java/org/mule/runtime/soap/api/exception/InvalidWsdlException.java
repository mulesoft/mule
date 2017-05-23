/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
public class InvalidWsdlException extends MuleRuntimeException {

  public InvalidWsdlException(String message) {
    super(createStaticMessage(message));
  }

  public InvalidWsdlException(String message, Throwable cause) {
    super(createStaticMessage(message), cause);
  }
}
