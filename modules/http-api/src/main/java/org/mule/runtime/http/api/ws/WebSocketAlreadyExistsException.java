/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.api.annotation.Experimental;
import org.mule.runtime.api.exception.MuleException;

@Experimental
public class WebSocketAlreadyExistsException extends MuleException {

  public WebSocketAlreadyExistsException(String id, WebSocket previous) {
    super(createStaticMessage(format(
                                     "A WebSocket connection for id '%s' already exists. Previous connection info:\n%s",
                                     id, previous.toString())));
  }
}
