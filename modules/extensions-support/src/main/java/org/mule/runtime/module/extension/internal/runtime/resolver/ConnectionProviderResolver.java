/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;

import java.util.Optional;

/**
 * A {@link ValueResolver} specialization for producing {@link ConnectionProvider} instances through a
 * {@link ConnectionProviderObjectBuilder}
 *
 * @since 4.0
 */
public class ConnectionProviderResolver<C> extends AbstractAnnotatedObject
    implements ConnectionProviderValueResolver<C>, Initialisable {

  private final ConnectionProviderObjectBuilder<C> objectBuilder;
  private final ObjectBuilderValueResolver<ConnectionProvider<C>> valueResolver;
  private final ResolverSet resolverSet;

  /**
   * Creates a new instance
   *
   * @param objectBuilder an object builder to instantiate the {@link ConnectionProvider}
   */
  public ConnectionProviderResolver(ConnectionProviderObjectBuilder<C> objectBuilder, ResolverSet resolverSet,
                                    MuleContext muleContext) {
    this.objectBuilder = objectBuilder;
    this.valueResolver = new ObjectBuilderValueResolver<>(objectBuilder, muleContext);
    this.resolverSet = resolverSet;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionProvider<C> resolve(Event event) throws MuleException {
    return valueResolver.resolve(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return valueResolver.isDynamic();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ResolverSet> getResolverSet() {
    return Optional.of(resolverSet);
  }

  public void setOwnerConfigName(String ownerConfigName) {
    objectBuilder.setOwnerConfigName(ownerConfigName);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(resolverSet);
  }
}
