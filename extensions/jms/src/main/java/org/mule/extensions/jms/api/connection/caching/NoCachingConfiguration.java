/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.caching;

import static java.util.Optional.empty;
import org.mule.runtime.extension.api.annotation.Alias;

import java.util.Optional;

import javax.jms.ConnectionFactory;

/**
 *
 * Implementation of {@link CachingConfiguration} that <b>disables</b> session caching
 * This {@link CachingConfiguration} is the recommended only if an external {@code CachingConnectionFactory}
 * is already being parametrized or if this extension is being used in the context of a Java EE web or EJB application.
 *
 * @since 4,0
 */
@Alias("no-caching")
public final class NoCachingConfiguration implements CachingStrategy {

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean appliesTo(ConnectionFactory target) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<CachingConfiguration> strategyConfiguration() {
    return empty();
  }
}
