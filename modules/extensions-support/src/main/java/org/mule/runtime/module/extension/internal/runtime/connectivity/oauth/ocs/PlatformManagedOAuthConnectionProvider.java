/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver.fromValues;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ConnectionUtils;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantTypeVisitor;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials.UpdatingClientCredentialsState;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.oauth.api.PlatformManagedConnectionDescriptor;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.slf4j.Logger;

public class PlatformManagedOAuthConnectionProvider<C> implements ConnectionProviderWrapper<C> {

  private static final Logger LOGGER = getLogger(PlatformManagedOAuthConnectionProvider.class);

  private final PlatformManagedOAuthConfig oauthConfig;
  private final PlatformManagedOAuthHandler oauthHandler;
  private final Pair<ConnectionProviderModel, OAuthGrantType> delegateModel;
  private final PoolingProfile poolingProfile;
  private final ReconnectionConfig reconnectionConfig;

  @Inject
  private MuleContext muleContext;

  @Inject
  private ExpressionManager expressionManager;

  private PlatformManagedOAuthDancer dancer;
  private ConnectionProvider delegate;
  private FieldSetter<ConnectionProvider<C>, OAuthState> oauthStateFieldSetter;

  public PlatformManagedOAuthConnectionProvider(PlatformManagedOAuthConfig oauthConfig,
                                                Pair<ConnectionProviderModel, OAuthGrantType> delegateModel,
                                                PlatformManagedOAuthHandler oauthHandler,
                                                ReconnectionConfig reconnectionConfig,
                                                PoolingProfile poolingProfile) {
    this.oauthConfig = oauthConfig;
    this.delegateModel = delegateModel;
    this.oauthHandler = oauthHandler;
    this.reconnectionConfig = reconnectionConfig;
    this.poolingProfile = poolingProfile;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(getRetryPolicyTemplate(), true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    dancer = oauthHandler.register(oauthConfig);

    try {
      PlatformManagedConnectionDescriptor descriptor = fetchConnectionDescriptor();
      delegate = createDelegate(descriptor);
      initialiseDelegate(descriptor);
      startIfNeeded(getRetryPolicyTemplate());
    } catch (MuleException e) {
      stopIfNeeded(dancer);
      disposeIfNeeded(dancer, LOGGER);
      throw e;
    }
  }

  private void initialiseDelegate(PlatformManagedConnectionDescriptor descriptor) throws MuleException {
    initialiseIfNeeded(delegate, true, muleContext);
    try {
      startIfNeeded(delegate);
    } catch (MuleException e) {
      disposeIfNeeded(delegate, LOGGER);
      throw e;
    }

    oauthStateFieldSetter = getOAuthStateSetter(delegate);
  }

  private ConnectionProvider<C> createDelegate(PlatformManagedConnectionDescriptor descriptor) throws MuleException {
    ParametersResolver resolver = fromValues(descriptor.getParameters(),
                                             muleContext,
                                             false,
                                             new ReflectionCache(),
                                             expressionManager);

    return (ConnectionProvider<C>) withContextClassLoader(getClassLoader(oauthConfig.getExtensionModel()), () -> {
      ResolverSet delegateResolverSet =
          resolver.getParametersAsResolverSet(oauthConfig.getDelegateConnectionProviderModel(), muleContext);
      ConnectionProviderObjectBuilder builder =
          new DefaultConnectionProviderObjectBuilder<>(oauthConfig.getDelegateConnectionProviderModel(),
                                                       delegateResolverSet,
                                                       poolingProfile,
                                                       reconnectionConfig,
                                                       oauthConfig.getExtensionModel(),
                                                       expressionManager,
                                                       muleContext);

      CoreEvent event = getNullEvent(muleContext);
      ValueResolvingContext ctx = null;
      try {
        ctx = ValueResolvingContext.builder(event, expressionManager)
            .withConfig(oauthConfig.getConfigurationInstance())
            .build();

        return builder.build(ctx);
      } finally {
        ((BaseEventContext) event.getContext()).success();
        if (ctx != null) {
          ctx.close();
        }
      }
    }, MuleException.class, e -> e);
  }

  private PlatformManagedConnectionDescriptor fetchConnectionDescriptor() throws MuleException {
    try {
      return dancer.getConnectionDescriptor().get();
    } catch (ExecutionException e) {
      throw newConnectionDescriptorException(e.getCause());
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw newConnectionDescriptorException(e);
    }
  }

  private MuleException newConnectionDescriptorException(Throwable e) {
    return new DefaultMuleException(
        "Could not obtain descriptor for Platform Managed OAuth Connection " + oauthConfig.getConnectionUri(), e);
  }

  private FieldSetter<ConnectionProvider<C>, OAuthState> getOAuthStateSetter(ConnectionProvider<C> delegate) {
    Reference<FieldSetter<ConnectionProvider<C>, ? extends OAuthState>> setter = new Reference<>();
    oauthConfig.getDelegateGrantType().accept(new OAuthGrantTypeVisitor() {

      @Override
      public void visit(AuthorizationCodeGrantType grantType) {
        setter.set(ExtensionsOAuthUtils.getOAuthStateSetter(delegate, AuthorizationCodeState.class, oauthConfig.getGrantType()));
      }

      @Override
      public void visit(ClientCredentialsGrantType grantType) {
        setter.set(ExtensionsOAuthUtils.getOAuthStateSetter(delegate, ClientCredentialsState.class, oauthConfig.getGrantType()));
      }

      @Override
      public void visit(PlatformManagedOAuthGrantType grantType) {
        throw illegalDelegateException();
      }
    });

    return (FieldSetter<ConnectionProvider<C>, OAuthState>) setter.get();
  }

  private IllegalConnectionProviderModelDefinitionException illegalDelegateException() {
    return new IllegalConnectionProviderModelDefinitionException(format(
        "Configuration '%s' cannot have a platform managed OAuth connection that delegates into itself",
        oauthConfig.getOwnerConfigName()));
  }



  @Override
  public C connect() throws ConnectionException {
    dance.runOnce();
    return super.connect();
  }

  @Override
  public ConnectionValidationResult validate(C connection) {
    try {
      ResourceOwnerOAuthContext context = getContext();
      if (context.getAccessToken() != null) {
        return getDelegate().validate(connection);
      } else {
        String message = "Server did not granted an access token";
        return failure(message, new IllegalStateException(message));
      }
    } catch (Exception e) {
      return failure("Could not obtain an access token", e);
    }
  }

  @Override
  public void refreshToken(String resourceOwnerId) {
    oauthHandler.refreshToken(oauthConfig);
  }

  @Override
  public void invalidate(String resourceOwnerId) {
    oauthHandler.invalidate(oauthConfig);
  }

  @Override
  public OAuthGrantType getGrantType() {
    return oauthConfig.getGrantType();
  }

  private void updateOAuthState() {
    oauthConfig.getDelegateGrantType().accept(new OAuthGrantTypeVisitor() {

      @Override
      public void visit(AuthorizationCodeGrantType grantType) {
        oauthStateFieldSetter.set(delegate, new PlatformAuthorizationCodeStateAdapter(dancer, descriptor));
      }

      @Override
      public void visit(ClientCredentialsGrantType grantType) {
        oauthStateFieldSetter.set(delegate, new PlatformClientCredentialsOAuthStateAdapter(dancer));
      }

      @Override
      public void visit(PlatformManagedOAuthGrantType grantType) {
        throw illegalDelegateException();
      }
    });

    // TODO: Figure out the consumer and the callbackValues
    final ConnectionProvider<C> delegate = getDelegate();
    ResourceOwnerOAuthContext context = getContext();
    oauthStateSetter.set(delegate, new UpdatingClientCredentialsState(
        dancer,
        context,
        updatedContext -> updateOAuthParameters(delegate,
                                                updatedContext)));

    updateOAuthParameters(delegate, context);
  }

  private ResourceOwnerOAuthContext getContext() {
    return oauthHandler.getOAuthContext(oauthConfig);
  }

  @Override
  public Optional<PoolingProfile> getPoolingProfile() {
    return ofNullable(poolingProfile);
  }

  @Override
  public Optional<ReconnectionConfig> getReconnectionConfig() {
    return ofNullable(reconnectionConfig);
  }

  @Override
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return ConnectionUtils.getRetryPolicyTemplate(getReconnectionConfig());
  }
}
