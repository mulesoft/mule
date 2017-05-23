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
 * Exception thrown when a reference to a server fails.
 *
 * @since 4.0
 */
public class ServerNotFoundException extends MuleException {

  private static final long serialVersionUID = 2587466114314625848L;

  public ServerNotFoundException(String serverName) {
    super(createStaticMessage("Server '%s' could not be found.", serverName));
  }

}
