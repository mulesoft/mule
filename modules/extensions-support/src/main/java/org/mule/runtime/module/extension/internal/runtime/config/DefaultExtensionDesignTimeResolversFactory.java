/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver.fromValues;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getResolverSetFromComponentParameterization;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.api.runtime.config.ExtensionDesignTimeResolversFactory;
import org.mule.runtime.module.extension.api.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.tooling.metadata.MetadataMediator;
import org.mule.runtime.module.extension.api.tooling.sampledata.SampleDataProviderMediator;
import org.mule.runtime.module.extension.api.tooling.valueprovider.ValueProviderMediator;
import org.mule.runtime.module.extension.internal.data.sample.DefaultSampleDataProviderMediator;
import org.mule.runtime.module.extension.internal.metadata.DefaultMetadataMediator;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionProviderSettings;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials.ClientCredentialsOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs.PlatformManagedOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.extension.internal.value.DefaultValueProviderMediator;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;


public class DefaultExtensionDesignTimeResolversFactory implements ExtensionDesignTimeResolversFactory {

  @Inject
  private MuleContext muleContext;

  @Inject
  private ReflectionCache reflectionCache;

  @Inject
  private ExpressionManager expressionManager;

  @Inject
  private Registry registry;

  @Override
  public <C> ConnectionProviderValueResolver<C> createConnectionProviderResolver(ConnectionProviderModel connectionProviderModel,
                                                                          ComponentParameterization componentParameterization,
                                                                          PoolingProfile poolingProfile,
                                                                          ReconnectionConfig reconnectionConfig,
                                                                          ExtensionModel extensionModel,
                                                                          ConfigurationProperties configurationProperties,
                                                                          String parametersOwner,
                                                                          DslSyntaxResolver dslSyntaxResolver)
      throws MuleException {
    ConnectionProviderSettings settings = new ConnectionProviderSettings(connectionProviderModel,
                                                                         componentParameterization,
                                                                         poolingProfile,
                                                                         reconnectionConfig,
                                                                         // these are looked-up lazily
                                                                         lookup(AuthorizationCodeOAuthHandler.class),
                                                                         lookup(ClientCredentialsOAuthHandler.class),
                                                                         lookup(PlatformManagedOAuthHandler.class));

    return ConfigurationCreationUtils.createConnectionProviderResolver(extensionModel,
                                                                       settings,
                                                                       configurationProperties,
                                                                       expressionManager,
                                                                       reflectionCache,
                                                                       parametersOwner,
                                                                       dslSyntaxResolver,
                                                                       muleContext);
  }

  private <T> T lookup(Class<T> clazz) {
    return registry.lookupByType(clazz).orElse(null);
  }

  @Override
  public ConfigurationProvider createConfigurationProvider(ExtensionModel extensionModel,
                                                           ConfigurationModel configurationModel,
                                                           String configName,
                                                           Map<String, Object> parameters,
                                                           Optional<ExpirationPolicy> expirationPolicy,
                                                           Optional<ConnectionProviderValueResolver> connectionProviderResolver,
                                                           ConfigurationProviderFactory configurationProviderFactory,
                                                           String parametersOwner,
                                                           DslSyntaxResolver dslSyntaxResolver,
                                                           ClassLoader extensionClassLoader) {
    return ConfigurationCreationUtils.createConfigurationProvider(extensionModel,
                                                                  configurationModel,
                                                                  configName,
                                                                  parameters,
                                                                  expirationPolicy,
                                                                  connectionProviderResolver,
                                                                  configurationProviderFactory,
                                                                  expressionManager,
                                                                  reflectionCache,
                                                                  parametersOwner,
                                                                  dslSyntaxResolver,
                                                                  extensionClassLoader,
                                                                  muleContext);
  }

  @Override
  public ValueProviderMediator createValueProviderMediator(ParameterizedModel parameterizedModel) {
    return new DefaultValueProviderMediator(parameterizedModel,
                                            () -> muleContext,
                                            () -> reflectionCache);
  }

  @Override
  public ParameterValueResolver createParameterValueResolver(ComponentParameterization<?> actingParameter,
                                                             ParameterizedModel parameterizedModel)
      throws MuleException {
    ResolverSet resolverSet = getResolverSetFromComponentParameterization(actingParameter,
                                                                          muleContext,
                                                                          true,
                                                                          reflectionCache,
                                                                          expressionManager,
                                                                          parameterizedModel.getName());
    return new ResolverSetBasedParameterResolver(resolverSet,
                                                 parameterizedModel,
                                                 reflectionCache,
                                                 expressionManager);

  }

  @Override
  public ResolverSet createParametersResolverSetFromValues(Map<String, ?> values, ParameterizedModel parameterizedModel)
      throws ConfigurationException {
    final ParametersResolver parametersResolver = fromValues(values,
                                                             muleContext,
                                                             true,
                                                             reflectionCache,
                                                             expressionManager,
                                                             parameterizedModel.getName());

    return parametersResolver.getParametersAsResolverSet(muleContext,
                                                         parameterizedModel,
                                                         parameterizedModel.getParameterGroupModels());
  }

  public SampleDataProviderMediator createSampleDataProviderMediator(ExtensionModel extensionModel,
                                                                     ComponentModel componentModel,
                                                                     Component component,
                                                                     StreamingManager streamingManager) {
    return new DefaultSampleDataProviderMediator(extensionModel,
                                                 componentModel,
                                                 component,
                                                 muleContext,
                                                 reflectionCache,
                                                 streamingManager);
  }

  @Override
  public <CM extends ComponentModel> MetadataMediator createMetadataMediator(CM componentModel) {
    return new DefaultMetadataMediator<>(componentModel,
                                         reflectionCache);
  }

}
