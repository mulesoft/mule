/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

/**
 * Exception thrown when a server cannot be created because a conflicting one already exists.
 *
 * @since 4.0
 */
public class ServerAlreadyExistsException extends ServerCreationException {

  public ServerAlreadyExistsException(String message) {
    super(message);
  }

}
