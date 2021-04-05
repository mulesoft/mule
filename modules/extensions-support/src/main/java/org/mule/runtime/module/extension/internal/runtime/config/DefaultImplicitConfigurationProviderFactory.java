/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.canBeUsedImplicitly;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getConnectedComponents;
import static org.mule.runtime.module.extension.internal.loader.utils.ImplicitObjectUtils.buildImplicitResolverSet;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.config.ImmutableExpirationPolicy;
import org.mule.runtime.core.internal.time.LocalTimeSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;
import org.mule.runtime.module.extension.internal.runtime.resolver.ImplicitConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import javax.inject.Inject;
import java.util.concurrent.Callable;

/**
 * Default implementation of {@link ImplicitConfigurationProviderFactory}. Implicit configurations are created from
 * {@link ConfigurationModel configurations} which have all parameters that are either not required or have a default value
 * defined that's not {@code null}.
 *
 * @since 3.8.0
 */
public final class DefaultImplicitConfigurationProviderFactory implements ImplicitConfigurationProviderFactory {

  @Inject
  FeatureFlaggingService featureFlaggingService;

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationProvider createImplicitConfigurationProvider(ExtensionModel extensionModel,
                                                                   ConfigurationModel configurationModel,
                                                                   CoreEvent event,
                                                                   ReflectionCache reflectionCache,
                                                                   ExpressionManager expressionManager,
                                                                   MuleContext muleContext) {
    if (configurationModel == null || !canBeUsedImplicitly(configurationModel)) {
      throw new IllegalStateException("Could not find a config for extension '" + extensionModel.getName()
          + "' and none can be created automatically. Please define one");
    }
    Callable<ResolverSet> resolverSetCallable =
        () -> buildImplicitResolverSet(configurationModel, reflectionCache, expressionManager, muleContext);
    ClassLoader pluginClassloader = getClassLoader(extensionModel);
    final ResolverSet resolverSet = withContextClassLoader(pluginClassloader, resolverSetCallable);
    try {
      final String providerName = resolveImplicitConfigurationProviderName(extensionModel, configurationModel, muleContext);
      ImplicitConnectionProviderValueResolver implicitConnectionProviderValueResolver =
          new ImplicitConnectionProviderValueResolver(configurationModel.getName(), extensionModel,
                                                      configurationModel, reflectionCache, expressionManager,
                                                      muleContext);

      ConfigurationInstance configurationInstance =
          withContextClassLoader(pluginClassloader, () -> new ConfigurationInstanceFactory(extensionModel,
                                                                                           configurationModel,
                                                                                           resolverSet,
                                                                                           expressionManager,
                                                                                           muleContext)
                                                                                               .createConfiguration(providerName,
                                                                                                                    event,
                                                                                                                    implicitConnectionProviderValueResolver));

      if (resolverSet.isDynamic() || needsDynamicConnectionProvider(extensionModel, configurationModel,
                                                                    implicitConnectionProviderValueResolver)) {
        return new DynamicConfigurationProvider(providerName, extensionModel, configurationModel,
                                                resolverSet,
                                                implicitConnectionProviderValueResolver,
                                                ImmutableExpirationPolicy.getDefault(new LocalTimeSupplier()), reflectionCache,
                                                expressionManager, muleContext);
      }

      return new ConfigurationProviderToolingAdapter(providerName, extensionModel,
                                                     configurationModel, configurationInstance, reflectionCache,
                                                     muleContext);

    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(format("Could not create an implicit configuration '%s' for the extension '%s'",
                                                                configurationModel.getName(),
                                                                extensionModel.getName())),
                                     e);
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

  public String resolveImplicitConfigurationProviderName(ExtensionModel extensionModel, ConfigurationModel configurationModel,
                                                         MuleContext muleContext) {
    if (muleContext.getArtifactType().equals(ArtifactType.POLICY)) {
      return featureFlaggedPolicyExtensionsImplicitConfigurationProviderName(extensionModel, configurationModel, muleContext);
    } else {
      return getImplicitConfigurationProviderName(extensionModel, configurationModel);
    }
  }

  /**
   * Resolves the {@link ConfigurationProvider} name (that can be used to look it up in the MuleContext registry) for a policy
   * extension, taking into account the {@link MuleRuntimeFeature.ENABLE_POLICY_ISOLATION} feature flag.
   * 
   * @param extensionModel     The {@link ExtensionModel} that must be configured.
   * @param configurationModel The {@link ConfigurationModel} that represents the extensionModel configuration.
   * @param muleContext        The corresponding {@link MuleContext}.
   * @return The {@link ConfigurationProvider} name.
   */
  private String featureFlaggedPolicyExtensionsImplicitConfigurationProviderName(ExtensionModel extensionModel,
                                                                                 ConfigurationModel configurationModel,
                                                                                 MuleContext muleContext) {
    if (featureFlaggingService != null && featureFlaggingService.isEnabled(MuleRuntimeFeature.ENABLE_POLICY_ISOLATION)) {
      // Implicit configuration providers cannot be inherited from the parent (application) MuleContext registry, so a different
      // name is returned,
      // which will force the instantiation of a new ConfigurationProvider that will be stored in the policy MuleContext registry.
      return String.format("%s-%s", muleContext.getId(),
                           getImplicitConfigurationProviderName(extensionModel, configurationModel));
    } else {
      // Implicit configuration provider is inherited from the parent (application) MuleContext registry.
      return getImplicitConfigurationProviderName(extensionModel, configurationModel);
    }
  }

  private String getImplicitConfigurationProviderName(ExtensionModel extensionModel,
                                                      ConfigurationModel implicitConfigurationModel) {
    return format("%s-%s-implicit", extensionModel.getName(), implicitConfigurationModel.getName());
  }

}
