/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.error;

import static org.mule.extension.http.api.error.HttpError.PARSING;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Exception thrown when there is a problem parsing an HTTP message.
 *
 * @since 4.0
 */
public class HttpMessageParsingException extends ModuleException {

  private static final long serialVersionUID = 2516645632512321036L;

  public HttpMessageParsingException(I18nMessage message, Throwable throwable) {
    super(throwable, PARSING, message);
  }

}
