/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;

/**
 * A {@link ValueResolver} specialization for producing {@link ConnectionProvider} instances through a
 * {@link ConnectionProviderObjectBuilder}
 *
 * @since 4.0
 */
public class ConnectionProviderResolver<C> extends AbstractAnnotatedObject implements ValueResolver<ConnectionProvider<C>> {

  private final ConnectionProviderObjectBuilder<C> objectBuilder;
  private final ObjectBuilderValueResolver<ConnectionProvider<C>> valueResolver;

  /**
   * Creates a new instance
   *
   * @param objectBuilder an object builder to instantiate the {@link ConnectionProvider}
   */
  public ConnectionProviderResolver(ConnectionProviderObjectBuilder<C> objectBuilder) {
    this.objectBuilder = objectBuilder;
    this.valueResolver = new ObjectBuilderValueResolver<>(objectBuilder);
  }

  @Override
  public ConnectionProvider<C> resolve(Event event) throws MuleException {
    return valueResolver.resolve(event);
  }

  @Override
  public boolean isDynamic() {
    return valueResolver.isDynamic();
  }

  public void setOwnerConfigName(String ownerConfigName) {
    objectBuilder.setOwnerConfigName(ownerConfigName);
  }
}
