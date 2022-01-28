package org.mule.runtime.http2.api.exception;

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleException;

public class Http2ServerCreationException extends MuleException {

  private static final long serialVersionUID = 1118746008940455831L;

  public Http2ServerCreationException(String message, Throwable cause) {
    super(createStaticMessage("Could not create server: " + message), cause);
  }

  public Http2ServerCreationException(String message) {
    super(createStaticMessage("Could not create server: " + message));
  }
}
