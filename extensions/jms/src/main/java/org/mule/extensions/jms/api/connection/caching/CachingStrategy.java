/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.caching;

import java.util.Optional;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;

/**
 * Defines the strategy to be used for caching of {@link Session}s and {@link Connection}s
 *
 * @since 4.0
 */
public interface CachingStrategy {

  /**
   * We only wrap connection factories that:
   *  - aren't instances of XAConnectionFactory
   *  - haven't already been wrapped
   *  - aren't already a caching factory
   * @param target the {@link ConnectionFactory} that will be wrapped using this {@link CachingConfiguration}
   * @return {@code true} if the {@code target} factory can be wrapped in a caching connection factory
   */
  boolean appliesTo(ConnectionFactory target);

  /**
   * @return the {@link CachingConfiguration} required for executing this {@link CachingStrategy}
   * if one is required.
   */
  Optional<CachingConfiguration> strategyConfiguration();

}
