/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleException;

/**
 * Exception thrown when a reference to a server fails.
 *
 * @since 4.0
 */
public final class ServerNotFoundException extends MuleException {

  private static final long serialVersionUID = 2587466114314625851L;

  public ServerNotFoundException(String serverName) {
    super(createStaticMessage("Server '%s' could not be found.", serverName));
  }

}
