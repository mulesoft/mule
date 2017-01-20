/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Exception thrown when there is a problem processing an HTTP message.
 *
 * @since 4.0
 */
public class HttpMessageParsingException extends MuleException {

  public HttpMessageParsingException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

}
