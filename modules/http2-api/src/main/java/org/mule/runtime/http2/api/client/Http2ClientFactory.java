/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.client;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http2.api.exception.Http2ClientCreationException;

import java.util.function.Supplier;

/**
 * Factory object for {@link Http2Client}.
 *
 * @since 4.5
 */
@NoImplement
public interface Http2ClientFactory {

  /**
   * If an {@link Http2Client} exists for the given name, it returns it. Otherwise, it creates a new {@link Http2Client} by using
   * the given {@link Http2ClientConfiguration} supplier.
   * <p>
   * In the case of returning an existent instance, it doesn't call the {@link Supplier#get()} method on the given supplier.
   *
   * @param name          the name of the required client.
   * @param configuration a supplier of the configuration for a new client if it needs to be created.
   * @return the {@link Http2Client} instance corresponding to the given name.
   * @throws Http2ClientCreationException if an error occurs while creating the new {@link Http2Client}
   */
  Http2Client getOrCreateClient(String name, Supplier<? extends Http2ClientConfiguration> configuration)
      throws Http2ClientCreationException;
}
