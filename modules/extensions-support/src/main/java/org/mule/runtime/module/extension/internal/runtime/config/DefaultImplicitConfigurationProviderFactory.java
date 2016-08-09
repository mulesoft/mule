/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.module.extension.internal.introspection.utils.ImplicitObjectUtils.buildImplicitResolverSet;
import static org.mule.runtime.module.extension.internal.introspection.utils.ImplicitObjectUtils.getFirstImplicit;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.ImmutableExpirationPolicy;
import org.mule.runtime.module.extension.internal.runtime.resolver.ImplicitConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * Default implementation of {@link ImplicitConfigurationProviderFactory}. Implicit configurations are created from
 * {@link ConfigurationModel configurations} which have all parameters that are either not required or have a default value
 * defined that's not {@code null}.
 *
 * @since 3.8.0
 */
public final class DefaultImplicitConfigurationProviderFactory implements ImplicitConfigurationProviderFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> ConfigurationProvider<C> createImplicitConfigurationProvider(ExtensionModel extensionModel, MuleEvent event) {
    RuntimeConfigurationModel implicitConfigurationModel =
        (RuntimeConfigurationModel) getFirstImplicit(extensionModel.getConfigurationModels());

    if (implicitConfigurationModel == null) {
      throw new IllegalStateException(String.format(
                                                    "Could not find a config for extension '%s' and none can be created automatically. Please define one",
                                                    extensionModel.getName()));
    }

    final String providerName = String.format("%s-%s", extensionModel.getName(), implicitConfigurationModel.getName());
    final ResolverSet resolverSet =
        buildImplicitResolverSet(implicitConfigurationModel, event.getMuleContext().getExpressionManager());
    try {
      ConfigurationInstance<C> configurationInstance =
          new ConfigurationInstanceFactory<C>(implicitConfigurationModel, resolverSet).createConfiguration(providerName, event);
      String configName = configurationInstance.getName();
      RuntimeConfigurationModel configurationModel = configurationInstance.getModel();

      if (resolverSet.isDynamic()) {
        return new DynamicConfigurationProvider<>(configName, configurationModel, resolverSet,
                                                  new ImplicitConnectionProviderValueResolver(configName, configurationModel),
                                                  ImmutableExpirationPolicy.getDefault(new TimeSupplier()));
      }

      return new StaticConfigurationProvider<>(configName, configurationModel, configurationInstance);

    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }
}
