/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

/**
 * Factory object for {@link HttpServer}.
 *
 * @since 4.0
 */
public interface HttpServerFactory {

  /**
   * Creates a new {@link HttpServer}.
   *
   * @param configuration a {@link HttpServerConfiguration} specifying the desired server.
   * @return a newly built {@link HttpServer} based on the {@code configuration}.
   * @throws ServerCreationException if the server cannot be created
   */
  HttpServer create(HttpServerConfiguration configuration) throws ServerCreationException;

  /**
   * Allows to retrieve a previously created {@link HttpServer}, if used from the same context. Keep in mind lifecycle changes to
   * the retrieved instance won't take effect since only the owner of the server can modify it's status.
   *
   * @param name the name the desired {@link HttpServer} was given when created (see {@link HttpServerConfiguration#getName()})
   * @return the server found
   * @throws ServerNotFoundException when the desired server was not found
   */
  HttpServer lookup(String name) throws ServerNotFoundException;
}
