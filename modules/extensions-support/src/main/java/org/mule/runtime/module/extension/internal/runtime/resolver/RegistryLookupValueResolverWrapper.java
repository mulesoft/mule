/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.util.func.Once;

import javax.inject.Inject;

/**
 * Implementation of {@link ValueResolver} which accesses the mule registry and returns the value associated with the result of
 * another {@link ValueResolver}.
 * <p/>
 * Although the registry is mutable, the value associated to a given key is not meant to change. As a consequence,
 * {@link #isDynamic()} will delegate to the wrapped {@link ValueResolver}.
 *
 * @since 4.5.0
 */
public class RegistryLookupValueResolverWrapper<T> implements ValueResolver<T>, Initialisable, MuleContextAware {

  private final ValueResolver<String> delegate;
  private MuleContext muleContext;
  private Registry registry;

  /**
   * Construct a new instance and set the {@link ValueResolver} of the key that will be used to access the registry
   *
   * @param delegate a resolver for a not blank {@link String}
   */
  public RegistryLookupValueResolverWrapper(ValueResolver<String> delegate) {
    this.delegate = delegate;
  }

  /**
   * Returns the registry value associated with the result of the {@link #delegate} {@link ValueResolver}.
   *
   * @param context a {@link ValueResolvingContext}
   * @return the registry value associated with the key resolved from {@link #delegate}
   * @throws MuleException          if an error occurred fetching the value
   * @throws ConfigurationException if no object is registered under the key resolved from {@link #delegate}
   */
  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    String key = delegate.resolve(context);
    checkArgument(!isBlank(key), "A null or empty key was provided. Registry lookup cannot be performed with a blank key");

    return registry.<T>lookupByName(key)
        .orElseThrow(() -> new ConfigurationException(createStaticMessage(format("Element '%s' is not defined in the Mule Registry",
                                                                                 key))));
  }

  @Override
  public boolean isDynamic() {
    return delegate.isDynamic();
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(delegate, true, muleContext);
  }

  @Inject
  public void setRegistry(Registry registry) {
    this.registry = registry;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
