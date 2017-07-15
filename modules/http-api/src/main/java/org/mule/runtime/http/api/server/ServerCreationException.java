/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleException;

/**
 * Exception thrown when a server cannot be created.
 *
 * @since 4.0
 */
public class ServerCreationException extends MuleException {

  private static final long serialVersionUID = -7954287390178516553L;

  public ServerCreationException(String message, Throwable cause) {
    super(createStaticMessage("Could not create server: " + message), cause);
  }

  public ServerCreationException(String message) {
    super(createStaticMessage("Could not create server: " + message));
  }

}
