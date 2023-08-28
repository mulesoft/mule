/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import static java.lang.String.format;

/**
 * Exception thrown when a server cannot be created because a conflicting one already exists.
 *
 * @since 4.0
 */
public final class ServerAlreadyExistsException extends ServerCreationException {

  private static final long serialVersionUID = -3622168573395746884L;
  private static final String SERVER_ALREADY_EXISTS_FORMAT =
      "A server in port(%s) already exists for host(%s) or one overlapping it (0.0.0.0).";

  public ServerAlreadyExistsException(ServerAddress serverAddress) {
    super(format(SERVER_ALREADY_EXISTS_FORMAT, serverAddress.getPort(), serverAddress.getIp()));
  }

}
