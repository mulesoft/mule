/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.canBeUsedImplicitly;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getConnectedComponents;
import static org.mule.runtime.module.extension.internal.loader.utils.ImplicitObjectUtils.buildImplicitResolverSet;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplicitConfigurationProviderName;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.time.LocalTimeSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;
import org.mule.runtime.core.internal.config.ImmutableExpirationPolicy;
import org.mule.runtime.module.extension.internal.runtime.resolver.ImplicitConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import java.util.concurrent.Callable;

import javax.inject.Inject;

/**
 * Default implementation of {@link ImplicitConfigurationProviderFactory}. Implicit configurations are created from
 * {@link ConfigurationModel configurations} which have all parameters that are either not required or have a default value
 * defined that's not {@code null}.
 *
 * @since 3.8.0
 */
public final class DefaultImplicitConfigurationProviderFactory implements ImplicitConfigurationProviderFactory {


  @Inject
  private ConnectionManager connectionManager;

  @Inject
  private MuleMetadataService metadataService;

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationProvider createImplicitConfigurationProvider(ExtensionModel extensionModel,
                                                                   ConfigurationModel implicitConfigurationModel,
                                                                   CoreEvent event,
                                                                   MuleContext muleContext) {
    if (implicitConfigurationModel == null || !canBeUsedImplicitly(implicitConfigurationModel)) {
      throw new IllegalStateException("Could not find a config for extension '" + extensionModel.getName()
          + "' and none can be created automatically. Please define one");
    }

    final String providerName = getImplicitConfigurationProviderName(extensionModel, implicitConfigurationModel);
    Callable<ResolverSet> resolverSetCallable = () -> buildImplicitResolverSet(implicitConfigurationModel, muleContext);
    final ResolverSet resolverSet = withContextClassLoader(getClassLoader(extensionModel), resolverSetCallable);
    try {
      ImplicitConnectionProviderValueResolver implicitConnectionProviderValueResolver =
          new ImplicitConnectionProviderValueResolver(implicitConfigurationModel.getName(), extensionModel,
                                                      implicitConfigurationModel, muleContext);

      ConfigurationInstance configurationInstance =
          new ConfigurationInstanceFactory(extensionModel, implicitConfigurationModel, resolverSet, muleContext)
              .createConfiguration(providerName, event, implicitConnectionProviderValueResolver);

      if (resolverSet.isDynamic() || needsDynamicConnectionProvider(extensionModel, implicitConfigurationModel,
                                                                    implicitConnectionProviderValueResolver)) {
        return new DynamicConfigurationProvider(providerName, extensionModel, implicitConfigurationModel,
                                                resolverSet,
                                                implicitConnectionProviderValueResolver,
                                                ImmutableExpirationPolicy.getDefault(new LocalTimeSupplier()), muleContext);
      }

      return new ConfigurationProviderToolingAdapter(providerName, extensionModel,
                                                     implicitConfigurationModel, configurationInstance, muleContext);

    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * An dynamic connection provider is needed if there are at least one connected component
   * {@link ExtensionModelUtils#getConnectedComponents(ExtensionModel)} and the
   * {@link ImplicitConnectionProviderValueResolver#isDynamic()}
   * 
   * @param extensionModel
   * @param configurationModel
   * @param implicitConnectionProviderValueResolver
   * @return {@code true} if an implicit provider is need, false otherwise.
   */
  private boolean needsDynamicConnectionProvider(ExtensionModel extensionModel, ConfigurationModel configurationModel,
                                                 ImplicitConnectionProviderValueResolver implicitConnectionProviderValueResolver) {
    return !getConnectedComponents(extensionModel, configurationModel).isEmpty()
        && implicitConnectionProviderValueResolver.isDynamic();
  }
}
