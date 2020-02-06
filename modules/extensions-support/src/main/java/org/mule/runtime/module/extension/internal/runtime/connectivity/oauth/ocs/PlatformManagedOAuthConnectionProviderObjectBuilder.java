/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.PLATFORM_MANAGED_CONNECTION_ID_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs.PlatformManagedOAuthConfig.from;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantTypeVisitor;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.BaseOAuthConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

/**
 * A {@link BaseOAuthConnectionProviderObjectBuilder} implementation that yields {@link PlatformManagedOAuthConnectionProvider}
 * instances
 *
 * @param <C> the generic type of the produced connections
 * @since 4.3.0
 */
public class PlatformManagedOAuthConnectionProviderObjectBuilder<C> extends BaseOAuthConnectionProviderObjectBuilder<C> {

  private final PlatformManagedOAuthHandler platformHandler;
  private final PlatformManagedOAuthGrantType grantType;
  private final ConfigurationProperties configurationProperties;

  public PlatformManagedOAuthConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                                             ResolverSet resolverSet,
                                                             PoolingProfile poolingProfile,
                                                             ReconnectionConfig reconnectionConfig,
                                                             PlatformManagedOAuthGrantType grantType,
                                                             PlatformManagedOAuthHandler platformHandler,
                                                             ConfigurationProperties configurationProperties,
                                                             ExtensionModel extensionModel,
                                                             ExpressionManager expressionManager,
                                                             MuleContext muleContext) {
    super(providerModel, resolverSet, poolingProfile, reconnectionConfig, extensionModel, expressionManager, muleContext);
    this.platformHandler = platformHandler;
    this.grantType = grantType;
    this.configurationProperties = configurationProperties;
  }

  @Override
  public Pair<ConnectionProvider<C>, ResolverSetResult> build(ValueResolvingContext context) throws MuleException {
    final ResolverSetResult resolverSetResult = resolverSet.resolve(context);
    final Pair<ConnectionProviderModel, OAuthGrantType> delegateModel = getDelegateOAuthConnectionProviderModel(context);
    final String ownerConfigName = context.getConfig().get().getName();
    final String connectionUri = (String) context.getConfig().get()
        .getState()
        .getConnectionParameters()
        .get(PLATFORM_MANAGED_CONNECTION_ID_PARAMETER_NAME);

    final PlatformManagedOAuthConfig config = from(ownerConfigName,
                                                   connectionUri,
                                                   grantType,
                                                   context.getConfig().get(),
                                                   extensionModel,
                                                   delegateModel.getFirst(),
                                                   delegateModel.getSecond(),
                                                   configurationProperties);

    ConnectionProvider<C> provider = new PlatformManagedOAuthConnectionProvider<>(config,
                                                                                  platformHandler,
                                                                                  reconnectionConfig,
                                                                                  poolingProfile);

    return new Pair<>(provider, resolverSetResult);
  }

  private Pair<ConnectionProviderModel, OAuthGrantType> getDelegateOAuthConnectionProviderModel(ValueResolvingContext context) {
    Reference<Pair<ConnectionProviderModel, OAuthGrantType>> authCodePair = new Reference<>();
    Reference<Pair<ConnectionProviderModel, OAuthGrantType>> clientCredentialsPair = new Reference<>();

    for (ConnectionProviderModel cpModel : context.getConfig().get().getModel().getConnectionProviders()) {
      if (authCodePair.get() != null) {
        break;
      }

      OAuthModelProperty property = cpModel.getModelProperty(OAuthModelProperty.class).orElse(null);
      if (property != null) {
        property.getGrantTypes().get(0).accept(new OAuthGrantTypeVisitor() {

          @Override
          public void visit(AuthorizationCodeGrantType grantType) {
            authCodePair.set(new Pair<>(cpModel, grantType));
          }

          @Override
          public void visit(ClientCredentialsGrantType grantType) {
            clientCredentialsPair.set(new Pair<>(cpModel, grantType));
          }

          @Override
          public void visit(PlatformManagedOAuthGrantType grantType) {
            // no - op
          }
        });
      }
    }

    if (authCodePair.get() != null) {
      return authCodePair.get();
    } else {
      return clientCredentialsPair.get();
    }
  }
}
