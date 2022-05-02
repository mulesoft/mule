/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.Optional;

/**
 * A {@link ValueResolver} wrapper which returns a {@link ConfigurationProvider} obtained through a {@link ValueResolver
 * delegate}.
 *
 * @since 4.5
 */
public class ConfigurationProviderResolverWrapper
    implements ValueResolver<ConfigurationProvider>, Initialisable, MuleContextAware {

  private final ValueResolver<ConfigurationProvider> delegate;
  private MuleContext muleContext;

  public ConfigurationProviderResolverWrapper(ValueResolver<ConfigurationProvider> configurationProviderResolver) {
    this.delegate = configurationProviderResolver;
  }

  public ConfigurationProviderResolverWrapper(ConfigurationProvider configurationProvider) {
    this.delegate = new StaticValueResolver<>(configurationProvider);
  }

  @Override
  public ConfigurationProvider resolve(ValueResolvingContext context) throws MuleException {
    return delegate.resolve(context);
  }

  public Optional<ConfigurationProvider> resolve() throws MuleException {
    if (dependsOnEvent()) {
      return empty();
    }

    return of(((EventAgnosticValueResolver<ConfigurationProvider>) delegate).resolve());
  }

  @Override
  public boolean isDynamic() {
    return delegate.isDynamic();
  }

  public boolean dependsOnEvent() {
    return !(delegate instanceof EventAgnosticValueResolver);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(delegate, true, muleContext);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
