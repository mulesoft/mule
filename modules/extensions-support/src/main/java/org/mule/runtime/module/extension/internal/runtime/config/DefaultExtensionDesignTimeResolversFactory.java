/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver.fromValues;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getResolverSetFromComponentParameterization;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.RouterOutputMetadataContext;
import org.mule.runtime.api.metadata.ScopeOutputMetadataContext;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.api.runtime.config.ExtensionDesignTimeResolversFactory;
import org.mule.runtime.module.extension.api.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.api.tooling.metadata.MetadataMediator;
import org.mule.runtime.module.extension.api.tooling.sampledata.SampleDataProviderMediator;
import org.mule.runtime.module.extension.api.tooling.valueprovider.ValueProviderMediator;
import org.mule.runtime.module.extension.internal.data.sample.DefaultSampleDataProviderMediator;
import org.mule.runtime.module.extension.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.module.extension.internal.metadata.DefaultMetadataMediator;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionProviderSettings;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials.ClientCredentialsOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs.PlatformManagedOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.extension.internal.value.DefaultValueProviderMediator;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.inject.Inject;


public class DefaultExtensionDesignTimeResolversFactory implements ExtensionDesignTimeResolversFactory {

  @Inject
  private MuleContext muleContext;

  @Inject
  private ReflectionCache reflectionCache;

  @Inject
  private ArtifactEncoding artifactEncoding;

  @Inject
  private ExtendedExpressionManager expressionManager;

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
                                                                       muleContext, artifactEncoding);
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
                                                                  muleContext, artifactEncoding);
  }

  @Override
  public <C> ConnectionProvider<C> createConnectionProvider(ExtensionModel extensionModel,
                                                            ConnectionProviderModel connectionProviderModel,
                                                            Map<String, Object> parameters,
                                                            String parametersOwner)
      throws MuleException {

    // TODO W-10992158
    ResolverSet resolverSet = createParametersResolverSetFromValues(parameters, connectionProviderModel);
    resolverSet.initialise();

    ConnectionProviderResolver<C> connectionProviderResolver =
        new ConnectionProviderResolver<>(new DefaultConnectionProviderObjectBuilder<>(connectionProviderModel,
                                                                                      resolverSet,
                                                                                      extensionModel,
                                                                                      expressionManager,
                                                                                      muleContext),
                                         resolverSet, muleContext);
    connectionProviderResolver.setOwnerConfigName(parametersOwner);

    ConnectionProvider<C> connectionProvider = connectionProviderResolver
        .getObjectBuilder().get()
        .build(resolveResolverSet(muleContext, resolverSet))
        .getFirst();

    return connectionProvider;
  }

  private ResolverSetResult resolveResolverSet(final MuleContext muleContext, ResolverSet resolverSet) throws MuleException {
    ResolverSetResult result;
    CoreEvent initializerEvent = CoreEvent.nullEvent();
    try {
      result = resolverSet.resolve(ValueResolvingContext.builder(initializerEvent).build());
    } finally {
      ((BaseEventContext) initializerEvent.getContext()).success();
    }
    return result;
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
                                                                          muleContext.getInjector(),
                                                                          parameterizedModel.getName(), artifactEncoding);
    return new DesignTimeParameterValueResolver(resolverSet,
                                                parameterizedModel,
                                                reflectionCache,
                                                expressionManager);
  }

  @Override
  public ResolverSet createParametersResolverSetFromValues(Map<String, ?> values, ParameterizedModel parameterizedModel)
      throws ConfigurationException, InitialisationException {
    final ParametersResolver parametersResolver = fromValues(values,
                                                             muleContext,
                                                             muleContext.getInjector(),
                                                             true,
                                                             reflectionCache,
                                                             expressionManager,
                                                             parameterizedModel.getName());

    ResolverSet typeUnsafeResolverSet = parametersResolver.getParametersAsResolverSet(muleContext,
                                                                                      parameterizedModel,
                                                                                      parameterizedModel
                                                                                          .getParameterGroupModels());

    Map<String, ParameterModel> paramModels =
        parameterizedModel.getAllParameterModels().stream()
            .collect(toMap(ParameterModel::getName, identity()));

    ResolverSet typeSafeResolverSet = new ResolverSet(muleContext.getInjector());
    typeUnsafeResolverSet.getResolvers().forEach((paramName, resolver) -> {
      ParameterModel model = paramModels.get(paramName);
      if (model != null) {
        Optional<Class<Object>> clazz = getType(model.getType());
        if (clazz.isPresent()) {
          resolver = new TypeSafeValueResolverWrapper<>(resolver, clazz.get());
        }
      }

      typeSafeResolverSet.add(paramName, resolver);
    });

    typeSafeResolverSet.initialise();
    return typeSafeResolverSet;
  }

  @Override
  public ValueProviderMediator createValueProviderMediator(ParameterizedModel parameterizedModel) {
    return new DefaultValueProviderMediator(parameterizedModel,
                                            () -> reflectionCache,
                                            () -> expressionManager,
                                            () -> muleContext.getInjector());
  }

  @Override
  public SampleDataProviderMediator createSampleDataProviderMediator(ExtensionModel extensionModel,
                                                                     ComponentModel componentModel,
                                                                     Component component,
                                                                     StreamingManager streamingManager) {
    return new DefaultSampleDataProviderMediator(extensionModel,
                                                 componentModel,
                                                 component,
                                                 artifactEncoding,
                                                 muleContext.getNotificationManager(),
                                                 reflectionCache,
                                                 expressionManager,
                                                 streamingManager,
                                                 muleContext.getInjector(),
                                                 muleContext);
  }

  @Override
  public MetadataContext createMetadataContext(Supplier<Optional<ConfigurationInstance>> configurationSupplier,
                                               ConnectionManager connectionManager,
                                               MetadataCache cache,
                                               ClassTypeLoader typeLoader) {
    return new DefaultMetadataContext(configurationSupplier, connectionManager, cache, typeLoader);
  }

  @Override
  public MetadataContext createMetadataContext(Supplier<Optional<ConfigurationInstance>> configurationSupplier,
                                               ConnectionManager connectionManager, MetadataCache cache,
                                               ClassTypeLoader typeLoader,
                                               Optional<ScopeOutputMetadataContext> scopeOutputMetadataContext,
                                               Optional<RouterOutputMetadataContext> routerOutputMetadataContext) {
    return new DefaultMetadataContext(configurationSupplier, connectionManager, cache, typeLoader, scopeOutputMetadataContext,
                                      routerOutputMetadataContext);
  }

  @Override
  public <CM extends ComponentModel> MetadataMediator createMetadataMediator(CM componentModel) {
    return new DefaultMetadataMediator<>(componentModel,
                                         reflectionCache);
  }

}
