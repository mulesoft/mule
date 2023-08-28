/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  private static final long serialVersionUID = -7954287390178516550L;

  public ServerCreationException(String message, Throwable cause) {
    super(createStaticMessage("Could not create server: " + message), cause);
  }

  public ServerCreationException(String message) {
    super(createStaticMessage("Could not create server: " + message));
  }

}
