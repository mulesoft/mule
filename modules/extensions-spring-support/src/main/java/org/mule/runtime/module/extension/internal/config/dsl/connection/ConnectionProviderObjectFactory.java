/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.connection;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.runtime.config.ConfigurationCreationUtils.createConnectionProviderResolver;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionProviderSettings;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials.ClientCredentialsOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs.PlatformManagedOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.property.SoapExtensionModelProperty;
import org.mule.runtime.module.extension.soap.internal.loader.property.SoapExtensionModelProperty;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.SoapConnectionProviderObjectBuilder;

import javax.inject.Inject;

/**
 * A {@link AbstractExtensionObjectFactory} that produces {@link ConnectionProviderResolver} instances
 *
 * @since 4.0
 */
public class ConnectionProviderObjectFactory extends AbstractExtensionObjectFactory<ConnectionProviderResolver> {

  private final ConnectionProviderModel providerModel;
  private final ExtensionModel extensionModel;
  private final AuthorizationCodeOAuthHandler authCodeHandler;
  private final ClientCredentialsOAuthHandler clientCredentialsHandler;
  private final PlatformManagedOAuthHandler platformManagedOAuthHandler;

  private PoolingProfile poolingProfile = null;
  private ReconnectionConfig reconnectionConfig = ReconnectionConfig.getDefault();

  private DslSyntaxResolver dslSyntaxResolver;

  @Inject
  private ConfigurationProperties properties;

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private MuleContext muleContext;

  public ConnectionProviderObjectFactory(ConnectionProviderModel providerModel,
                                         ExtensionModel extensionModel,
                                         AuthorizationCodeOAuthHandler authCodeHandler,
                                         ClientCredentialsOAuthHandler clientCredentialsHandler,
                                         PlatformManagedOAuthHandler platformManagedOAuthHandler,
                                         MuleContext muleContext) {
    super(muleContext);
    this.providerModel = providerModel;
    this.extensionModel = extensionModel;
    this.authCodeHandler = authCodeHandler;
    this.clientCredentialsHandler = clientCredentialsHandler;
    this.platformManagedOAuthHandler = platformManagedOAuthHandler;
    dslSyntaxResolver = DslSyntaxResolver.getDefault(extensionModel,
        DslResolvingContext.getDefault(extensionManager.getExtensions()));
  }

  @Override
  public ConnectionProviderResolver doGetObject() {

    if (extensionModel.getModelProperty(SoapExtensionModelProperty.class).isPresent()) {
      ResolverSet resolverSet = withContextClassLoader(getClassLoader(extensionModel),
          () -> getParametersResolver().getParametersAsResolverSet(providerModel, muleContext));

      SoapConnectionProviderObjectBuilder builder = new SoapConnectionProviderObjectBuilder(
          providerModel,
          resolverSet,
          poolingProfile,
          reconnectionConfig,
          extensionModel,
          expressionManager,
          muleContext);

      return new ConnectionProviderResolver<>(builder, resolverSet, muleContext);
    }

    try {
      return createConnectionProviderResolver(
          extensionModel,
          getConnectionProviderSettings(),
          properties,
          expressionManager,
          reflectionCache,
          getRepresentation(),
          dslSyntaxResolver,
          muleContext
      );
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private ConnectionProviderSettings getConnectionProviderSettings() {
    return new ConnectionProviderSettings(
        providerModel,
        parameters,
        poolingProfile,
        reconnectionConfig,
        authCodeHandler,
        clientCredentialsHandler,
        platformManagedOAuthHandler
    );
  }

  public void setPoolingProfile(PoolingProfile poolingProfile) {
    this.poolingProfile = poolingProfile;
  }

  public void setReconnectionConfig(ReconnectionConfig reconnectionConfig) {
    this.reconnectionConfig = reconnectionConfig;
  }
}
