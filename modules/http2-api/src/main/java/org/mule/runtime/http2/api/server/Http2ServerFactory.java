/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.server;

import org.mule.runtime.http2.api.exception.Http2ServerCreationException;

import java.util.function.Supplier;

/**
 * Interface capable of creating new instance of {@link Http2Server} or returning already created ones. It has a method to get or
 * create instances instead of two separate methods in order to make the implementations easier to be thread-safe.
 */
public interface Http2ServerFactory {

  /**
   * If an {@link Http2Server} exists for the given name, it returns it. Otherwise, it creates a new {@link Http2Server} by using
   * the given {@link Http2ServerConfiguration} supplier.
   * <p>
   * In the case of returning an existent instance, it doesn't call the {@link Supplier#get()} method on the given supplier.
   *
   * @param name          the name of the required server.
   * @param configuration a supplier of the configuration for a new server if it needs to be created.
   * @return the {@link Http2Server} instance corresponding to the given name.
   * @throws Http2ServerCreationException if an error occurs while creating the new {@link Http2Server}
   */
  Http2Server getOrCreateServer(String name, Supplier<? extends Http2ServerConfiguration> configuration)
      throws Http2ServerCreationException;
}
