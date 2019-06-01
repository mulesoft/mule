/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.CLIENT_ID_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.CLIENT_SECRET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OAUTH_AUTHORIZATION_CODE_GROUP_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OAUTH_CLIENT_CREDENTIALS_GROUP_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.SCOPES_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.TOKEN_URL_PARAMETER_NAME;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.BaseOAuthConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * A specialization of {@link BaseOAuthConnectionProviderObjectBuilder} to wrap the {@link ConnectionProvider}
 * into {@link ClientCredentialsConnectionProviderWrapper} instances.
 *
 * @since 4.2.1
 */
public class ClientCredentialsConnectionProviderObjectBuilder<C> extends BaseOAuthConnectionProviderObjectBuilder<C> {

  private final ClientCredentialsOAuthHandler clientCredentialsHandler;
  private final ClientCredentialsGrantType grantType;
  private final Map<Field, String> callbackValues;

  public ClientCredentialsConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                                          ResolverSet resolverSet,
                                                          PoolingProfile poolingProfile,
                                                          ReconnectionConfig reconnectionConfig,
                                                          ClientCredentialsGrantType grantType,
                                                          ClientCredentialsOAuthHandler clientCredentialsHandler,
                                                          ExtensionModel extensionModel,
                                                          ExpressionManager expressionManager,
                                                          MuleContext muleContext) {
    super(providerModel, resolverSet, poolingProfile, reconnectionConfig, extensionModel, expressionManager, muleContext);
    this.clientCredentialsHandler = clientCredentialsHandler;
    this.grantType = grantType;
    callbackValues = getCallbackValues();
  }

  @Override
  protected ConnectionProvider<C> doBuild(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> provider = super.doBuild(result);
    provider = wrapProvider(result, provider, getClientCredentialsParams(result));

    return provider;
  }

  @Override
  public Pair<ConnectionProvider<C>, ResolverSetResult> build(ValueResolvingContext context) throws MuleException {
    ResolverSetResult result = resolverSet.resolve(context);
    ConnectionProvider<C> provider = super.doBuild(result);

    provider = wrapProvider(result, provider, getClientCredentialsParams(context.getEvent()));
    return new Pair<>(provider, result);
  }

  private ConnectionProvider<C> wrapProvider(ResolverSetResult result, ConnectionProvider<C> provider,
                                             Map<String, String> clientCredentialsParams) {
    CustomOAuthParameters customParameters = getCustomParameters(result);

    ClientCredentialsConfig config = new ClientCredentialsConfig(ownerConfigName,
                                                                 buildOAuthObjectStoreConfig(result),
                                                                 customParameters.getQueryParams(),
                                                                 customParameters.getHeaders(),
                                                                 callbackValues,
                                                                 clientCredentialsParams.get(CLIENT_ID_PARAMETER_NAME),
                                                                 clientCredentialsParams.get(CLIENT_SECRET_PARAMETER_NAME),
                                                                 clientCredentialsParams.get(TOKEN_URL_PARAMETER_NAME),
                                                                 clientCredentialsParams.get(SCOPES_PARAMETER_NAME),
                                                                 grantType.getCredentialsPlacement(),
                                                                 grantType);


    provider = new ClientCredentialsConnectionProviderWrapper<>(provider,
                                                                config,
                                                                callbackValues,
                                                                clientCredentialsHandler,
                                                                reconnectionConfig);
    return provider;
  }

  private Map<String, String> getClientCredentialsParams(ResolverSetResult result) {
    return (Map<String, String>) result.get(OAUTH_AUTHORIZATION_CODE_GROUP_NAME);
  }

  private Map<String, String> getClientCredentialsParams(CoreEvent event) throws MuleException {
    ValueResolver<?> valueResolver = resolverSet.getResolvers().get(OAUTH_CLIENT_CREDENTIALS_GROUP_NAME);
    try (ValueResolvingContext context = getResolvingContextFor(event)) {
      return (Map<String, String>) valueResolver.resolve(context);
    }
  }
}
