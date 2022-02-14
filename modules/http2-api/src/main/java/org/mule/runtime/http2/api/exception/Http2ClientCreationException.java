package org.mule.runtime.http2.api.exception;

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleException;

public class Http2ClientCreationException extends MuleException {

  private static final long serialVersionUID = 6193336806882473739L;

  public Http2ClientCreationException(String message, Throwable cause) {
    super(createStaticMessage("Could not create client: " + message), cause);
  }

  public Http2ClientCreationException(String message) {
    super(createStaticMessage("Could not create client: " + message));
  }
}
