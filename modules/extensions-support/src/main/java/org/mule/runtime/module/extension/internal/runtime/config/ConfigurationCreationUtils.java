/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.supportsConnectivity;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getResolverSetFromComponentParameterization;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getResolverSetFromStaticValues;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantTypeVisitor;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.api.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionProviderSettings;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials.ClientCredentialsConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs.PlatformManagedOAuthConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ImplicitConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Map;
import java.util.Optional;

/**
 * Utilities for creating {@link ConfigurationProvider} instances and their associated {@link ConnectionProviderResolver}
 *
 * @since 4.5.0
 */
public final class ConfigurationCreationUtils {

  /**
   * Creates a {@link ConnectionProviderResolver}
   *
   * @param extensionModel               the model for the extension that owns the {@link ConnectionProvider}
   * @param configurationModel           the model for the configurationModel that owns the {@link ConnectionProvider}
   * @param configName                   the name of the configuration instance
   * @param parameters                   the parameter values
   * @param expirationPolicy             the {@link ExpirationPolicy} if the created config is dynamic
   * @param connectionProviderResolver   the {@link ConnectionProviderResolver}
   * @param configurationProviderFactory a {@link ConfigurationProviderFactory} used to instantiate the provider
   * @param expressionManager            an {@link ExpressionManager}
   * @param reflectionCache              a {@link ReflectionCache}
   * @param parametersOwner              the name of the owning component or an empty string
   * @param dslSyntaxResolver            a {@link DslSyntaxResolver}
   * @param extensionClassLoader         the extension's {@link ClassLoader}
   * @param muleContext                  the current {@link MuleContext}
   * @param artifactEncoding             the encoding used in the application
   * @return a new {@link ConfigurationProvider}
   * @throws MuleException if the resolver cannot be created
   * @since 4.5.0
   */
  public static ConfigurationProvider createConfigurationProvider(ExtensionModel extensionModel,
                                                                  ConfigurationModel configurationModel,
                                                                  String configName,
                                                                  Map<String, Object> parameters,
                                                                  Optional<ExpirationPolicy> expirationPolicy,
                                                                  Optional<ConnectionProviderValueResolver> connectionProviderResolver,
                                                                  ConfigurationProviderFactory configurationProviderFactory,
                                                                  ExpressionManager expressionManager,
                                                                  ReflectionCache reflectionCache,
                                                                  String parametersOwner,
                                                                  DslSyntaxResolver dslSyntaxResolver,
                                                                  ClassLoader extensionClassLoader,
                                                                  MuleContext muleContext, ArtifactEncoding artifactEncoding) {
    return withContextClassLoader(extensionClassLoader, () -> {
      ResolverSet resolverSet = getResolverSetFromStaticValues(
                                                               configurationModel,
                                                               parameters,
                                                               muleContext,
                                                               false,
                                                               reflectionCache,
                                                               expressionManager,
                                                               parametersOwner, artifactEncoding);

      final ConnectionProviderValueResolver connectionResolver = connectionProviderResolver
          .orElseGet(() -> supportsConnectivity(extensionModel, configurationModel)
              ? new ImplicitConnectionProviderValueResolver(configName, extensionModel, configurationModel, reflectionCache,
                                                            expressionManager, muleContext)
              : new StaticConnectionProviderResolver(null, null));

      connectionResolver.getResolverSet()
          .ifPresent((CheckedConsumer) resolver -> initialiseIfNeeded(resolver, true, muleContext));

      ConfigurationProvider configurationProvider;
      try {
        if (resolverSet.isDynamic() || connectionResolver.isDynamic()) {
          configurationProvider = configurationProviderFactory
              .createDynamicConfigurationProvider(configName,
                                                  extensionModel,
                                                  configurationModel,
                                                  resolverSet,
                                                  connectionResolver,
                                                  getActingExpirationPolicy(expirationPolicy, muleContext));
        } else {
          configurationProvider = configurationProviderFactory
              .createStaticConfigurationProvider(configName,
                                                 extensionModel,
                                                 configurationModel,
                                                 resolverSet,
                                                 connectionResolver);
        }
      } catch (Exception e) {
        throw new MuleRuntimeException(
                                       createStaticMessage(format("Could not create an implicit configuration '%s' for the extension '%s'",
                                                                  configurationModel.getName(), extensionModel.getName())),
                                       e);
      }
      return configurationProvider;
    });
  }

  /**
   * Creates a {@link ConnectionProviderResolver}
   *
   * @param extensionModel          the model for the extension that owns the {@link ConnectionProvider}
   * @param settings                a {@link ConnectionProviderSettings}
   * @param configurationProperties a {@link ConfigurationProperties}
   * @param expressionManager       an {@link ExpressionManager}
   * @param reflectionCache         a {@link ReflectionCache}
   * @param parametersOwner         the owner component name
   * @param dslSyntaxResolver       a {@link DslSyntaxResolver}
   * @param muleContext             the current {@link MuleContext}
   * @param artifactEncoding        the encoding used in the application
   * @return a new {@link ConnectionProviderResolver}
   * @param <C> the generic type of the provider's connection object
   * @throws MuleException if the resolver cannot be created
   * @since 4.5.0
   */
  public static <C> ConnectionProviderResolver<C> createConnectionProviderResolver(
                                                                                   ExtensionModel extensionModel,
                                                                                   ConnectionProviderSettings settings,
                                                                                   ConfigurationProperties configurationProperties,
                                                                                   ExpressionManager expressionManager,
                                                                                   ReflectionCache reflectionCache,
                                                                                   String parametersOwner,
                                                                                   DslSyntaxResolver dslSyntaxResolver,
                                                                                   MuleContext muleContext,
                                                                                   ArtifactEncoding artifactEncoding)
      throws MuleException {

    final ConnectionProviderModel providerModel = settings.getConnectionProviderModel();
    final ResolverSet resolverSet = getResolverSetFromComponentParameterization(
                                                                                settings.getParameters(),
                                                                                muleContext,
                                                                                false,
                                                                                reflectionCache,
                                                                                expressionManager,
                                                                                parametersOwner, artifactEncoding);

    BaseConnectionProviderObjectBuilder builder;
    if (providerModel.getModelProperty(OAuthModelProperty.class).isPresent()) {
      builder = resolveOAuthBuilder(extensionModel, providerModel, settings, resolverSet,
                                    configurationProperties, expressionManager, muleContext);
    } else {
      builder = new DefaultConnectionProviderObjectBuilder(providerModel, resolverSet,
                                                           settings.getPoolingProfile().orElse(null),
                                                           settings.getReconnectionConfig().orElse(null),
                                                           extensionModel,
                                                           expressionManager,
                                                           muleContext);
    }

    return new ConnectionProviderResolver<>(builder, resolverSet, muleContext);
  }

  private static BaseConnectionProviderObjectBuilder resolveOAuthBuilder(ExtensionModel extensionModel,
                                                                         ConnectionProviderModel providerModel,
                                                                         ConnectionProviderSettings settings,
                                                                         ResolverSet resolverSet,
                                                                         ConfigurationProperties configurationProperties,
                                                                         ExpressionManager expressionManager,
                                                                         MuleContext muleContext) {
    OAuthGrantType grantType = providerModel.getModelProperty(OAuthModelProperty.class)
        .map(OAuthModelProperty::getGrantTypes)
        .get().get(0);

    Reference<BaseConnectionProviderObjectBuilder> builder = new Reference<>();

    grantType.accept(new OAuthGrantTypeVisitor() {

      @Override
      public void visit(AuthorizationCodeGrantType grantType) {
        builder.set(new AuthorizationCodeConnectionProviderObjectBuilder(providerModel,
                                                                         resolverSet,
                                                                         settings.getPoolingProfile().orElse(null),
                                                                         settings.getReconnectionConfig().orElse(null),
                                                                         grantType,
                                                                         settings.getAuthorizationCodeOAuthHandler(),
                                                                         extensionModel,
                                                                         expressionManager,
                                                                         muleContext));
      }

      @Override
      public void visit(ClientCredentialsGrantType grantType) {
        builder.set(new ClientCredentialsConnectionProviderObjectBuilder(providerModel,
                                                                         resolverSet,
                                                                         settings.getPoolingProfile().orElse(null),
                                                                         settings.getReconnectionConfig().orElse(null),
                                                                         grantType,
                                                                         settings.getClientCredentialsOAuthHandler(),
                                                                         extensionModel,
                                                                         expressionManager,
                                                                         muleContext));
      }

      @Override
      public void visit(PlatformManagedOAuthGrantType grantType) {
        builder.set(new PlatformManagedOAuthConnectionProviderObjectBuilder(providerModel,
                                                                            resolverSet,
                                                                            settings.getPoolingProfile().orElse(null),
                                                                            settings.getReconnectionConfig().orElse(null),
                                                                            grantType,
                                                                            settings.getPlatformManagedOAuthHandler(),
                                                                            configurationProperties,
                                                                            extensionModel,
                                                                            expressionManager,
                                                                            muleContext));
      }
    });

    return builder.get();
  }

  private static ExpirationPolicy getActingExpirationPolicy(Optional<ExpirationPolicy> expirationPolicy,
                                                            MuleContext muleContext) {
    return expirationPolicy.orElseGet(() -> muleContext.getConfiguration().getDynamicConfigExpiration().getExpirationPolicy());
  }

  private ConfigurationCreationUtils() {}
}
