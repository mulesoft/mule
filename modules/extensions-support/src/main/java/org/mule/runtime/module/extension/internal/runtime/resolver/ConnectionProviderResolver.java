/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;

import java.util.Optional;

/**
 * A {@link ValueResolver} specialization for producing {@link ConnectionProvider} instances through a
 * {@link ConnectionProviderObjectBuilder}
 *
 * @since 4.0
 */
public class ConnectionProviderResolver<C> extends AbstractComponent
    implements ConnectionProviderValueResolver<C>, Initialisable, Startable {

  private final ConnectionProviderObjectBuilder<C> objectBuilder;
  private final ObjectBuilderValueResolver<Pair<ConnectionProvider<C>, ResolverSetResult>> valueResolver;
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
  public Pair<ConnectionProvider<C>, ResolverSetResult> resolve(ValueResolvingContext context) throws MuleException {
    return valueResolver.resolve(context);
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
    return of(resolverSet);
  }

  @Override
  public Optional<ConnectionProviderObjectBuilder<C>> getObjectBuilder() {
    return of(objectBuilder);
  }

  public void setOwnerConfigName(String ownerConfigName) {
    objectBuilder.setOwnerConfigName(ownerConfigName);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(resolverSet);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(objectBuilder);
  }
}
