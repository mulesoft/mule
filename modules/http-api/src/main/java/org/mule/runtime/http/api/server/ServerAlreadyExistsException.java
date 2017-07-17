/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import static java.lang.String.format;

/**
 * Exception thrown when a server cannot be created because a conflicting one already exists.
 *
 * @since 4.0
 */
public class ServerAlreadyExistsException extends ServerCreationException {

  private static final long serialVersionUID = -3622168573395746887L;
  private static final String SERVER_ALREADY_EXISTS_FORMAT =
      "A server in port(%s) already exists for host(%s) or one overlapping it (0.0.0.0).";

  public ServerAlreadyExistsException(ServerAddress serverAddress) {
    super(format(SERVER_ALREADY_EXISTS_FORMAT, serverAddress.getPort(), serverAddress.getIp()));
  }

}
