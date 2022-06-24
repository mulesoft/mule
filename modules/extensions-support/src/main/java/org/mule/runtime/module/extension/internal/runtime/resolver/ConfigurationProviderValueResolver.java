/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;


/**
 * Implementation of {@link ValueResolver} which locates and returns a {@link ConfigurationProvider} associated with a given
 * configuration provider reference. The reference can either be the name of the configuration provider or an expression which
 * resolves to the name.
 * <p/>
 * The result of {@link #isDynamic()} will depend only on whether the given reference is an expression or not.
 *
 * @since 4.5.0
 */
public class ConfigurationProviderValueResolver implements ValueResolver<ConfigurationProvider>, Initialisable, MuleContextAware {

  private final ValueResolver<String> configurationProviderNameResolver;

  private MuleContext muleContext;
  private ConfigurationProvider cachedConfigurationProvider;

  /**
   * Construct a new instance.
   *
   * @param configurationProviderReference a not blank {@link String}
   */
  public ConfigurationProviderValueResolver(String configurationProviderReference) {
    if (isExpression(configurationProviderReference)) {
      configurationProviderNameResolver =
          new TypeSafeExpressionValueResolver<>(configurationProviderReference, String.class, fromType(String.class));
    } else {
      configurationProviderNameResolver = new StaticValueResolver<>(configurationProviderReference);
    }
  }

  /**
   * Locates and returns the {@link ConfigurationProvider} associated with the {@code configurationProviderReference} given upon
   * construction.
   *
   * @param context a {@link ValueResolvingContext}
   * @return the {@link ConfigurationProvider} associated with {@code configurationProviderReference}
   * @throws MuleException          if an error occurred fetching the value
   * @throws ConfigurationException if no object is registered under the name resolved from {@code configurationProviderReference}
   */
  @Override
  public ConfigurationProvider resolve(ValueResolvingContext context) throws MuleException {
    if (cachedConfigurationProvider != null) {
      return cachedConfigurationProvider;
    }

    ConfigurationProvider configurationProvider = doResolve(context);
    if (!isDynamic()) {
      cachedConfigurationProvider = configurationProvider;
    }
    return configurationProvider;
  }

  private ConfigurationProvider doResolve(ValueResolvingContext context) throws MuleException {
    String configurationProviderName = configurationProviderNameResolver.resolve(context);
    return muleContext.getExtensionManager().getConfigurationProvider(configurationProviderName)
        .orElseThrow(() -> new ConfigurationException(createStaticMessage(format("There is no registered configurationProvider under name '%s'",
                                                                                 configurationProviderName))));
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return configurationProviderNameResolver.isDynamic();
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(configurationProviderNameResolver, muleContext);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    muleContext = context;
  }
}
