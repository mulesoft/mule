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
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.getCallbackValuesExtractors;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.updateOAuthParameters;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.validateOAuthConnection;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver.fromValues;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplementingType;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.core.api.util.func.Once.RunOnce;
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
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConnectionProviderWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.oauth.api.PlatformManagedConnectionDescriptor;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * An {@link OAuthConnectionProviderWrapper} for OAuth connections managed on the Anypoint Platform
 *
 * @param <C> the generic type of the generated connections
 * @since 4.3.0
 */
public class PlatformManagedOAuthConnectionProvider<C>
    implements OAuthConnectionProviderWrapper<C>, PoolingConnectionProvider<C> {

  private static final Logger LOGGER = getLogger(PlatformManagedOAuthConnectionProvider.class);

  private final PlatformManagedOAuthConfig oauthConfig;
  private final PlatformManagedOAuthHandler oauthHandler;
  private final PoolingProfile poolingProfile;
  private final ReconnectionConfig reconnectionConfig;
  private final Map<Field, String> callbackValues;
  private final RunOnce dance = Once.of(this::updateOAuthState);

  @Inject
  private MuleContext muleContext;

  @Inject
  private ExpressionManager expressionManager;

  private PlatformManagedOAuthDancer dancer;
  private ConnectionProvider<C> delegate;
  private ConnectionProvider<C> unwrappedDelegate;
  private FieldSetter<ConnectionProvider<C>, OAuthState> oauthStateFieldSetter;
  private PlatformManagedConnectionDescriptor descriptor;
  private PoolingListener<C> delegatePoolingListener;

  public PlatformManagedOAuthConnectionProvider(PlatformManagedOAuthConfig oauthConfig,
                                                PlatformManagedOAuthHandler oauthHandler,
                                                ReconnectionConfig reconnectionConfig,
                                                PoolingProfile poolingProfile) {
    this.oauthConfig = oauthConfig;
    this.oauthHandler = oauthHandler;
    this.reconnectionConfig = reconnectionConfig;
    this.poolingProfile = poolingProfile;
    callbackValues = getCallbackValuesExtractors(oauthConfig.getDelegateConnectionProviderModel());
  }

  @Override
  public C connect() throws ConnectionException {
    dance.runOnce();
    return ConnectionUtils.connect(getDelegate());
  }

  @Override
  public ConnectionValidationResult validate(C connection) {
    return validateOAuthConnection(getDelegate(), connection, getContext());
  }

  @Override
  public void disconnect(C connection) {
    getDelegate().disconnect(connection);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(getRetryPolicyTemplate(), true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    dancer = oauthHandler.register(oauthConfig);

    try {
      descriptor = fetchConnectionDescriptor();
      delegate = createDelegate(descriptor);
      unwrappedDelegate = unwrapConnectionProvider(delegate);
      delegatePoolingListener = getDelegatePoolingListener();
      initialiseDelegate();
      startIfNeeded(getRetryPolicyTemplate());
    } catch (MuleException e) {
      stopIfNeeded(dancer);
      disposeIfNeeded(dancer, LOGGER);
      throw e;
    }
  }

  private ConnectionProvider<C> unwrapConnectionProvider(ConnectionProvider<C> connectionProvider) {
    return connectionProvider instanceof ConnectionProviderWrapper
        ? unwrapConnectionProvider(((ConnectionProviderWrapper<C>) connectionProvider).getDelegate())
        : connectionProvider;
  }

  @Override
  public void stop() throws MuleException {
    try {
      stopIfNeeded(dancer);
    } finally {
      safely(() -> stopIfNeeded(getRetryPolicyTemplate()),
             e -> LOGGER.error(format("Error stopping %s for Platform Connection %s",
                                      RetryPolicyTemplate.class.getName(),
                                      descriptor.getDisplayName()),
                               e));
    }
  }

  @Override
  public void dispose() {
    disposeIfNeeded(dancer, LOGGER);
    disposeIfNeeded(getRetryPolicyTemplate(), LOGGER);
  }

  private void initialiseDelegate() throws MuleException {
    initialiseIfNeeded(delegate, true, muleContext);
    try {
      startIfNeeded(delegate);
    } catch (MuleException e) {
      disposeIfNeeded(delegate, LOGGER);
      throw e;
    }

    oauthStateFieldSetter = getOAuthStateSetter(unwrappedDelegate);
  }

  private ConnectionProvider<C> createDelegate(PlatformManagedConnectionDescriptor descriptor) throws MuleException {
    ParametersResolver resolver = fromValues(descriptor.getParameters(),
                                             muleContext,
                                             false,
                                             new ReflectionCache(),
                                             expressionManager);
    Class<? extends ConnectionProvider> connectionProviderDelegateClass =
        getImplementingType(oauthConfig.getDelegateConnectionProviderModel())
            .orElseThrow(() -> new IllegalStateException("Delegate connection provider must have an implementing type."));

    return (ConnectionProvider<C>) withContextClassLoader(getClassLoader(oauthConfig.getExtensionModel()), () -> {
      ResolverSet delegateResolverSet =
          resolver.getParametersAsResolverSet(oauthConfig.getDelegateConnectionProviderModel(), muleContext);
      ConnectionProviderObjectBuilder builder =
          new DefaultConnectionProviderObjectBuilder<>(connectionProviderDelegateClass,
                                                       oauthConfig.getDelegateConnectionProviderModel(),
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
            .build();

        return ((Pair) builder.build(ctx)).getFirst();
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
                                    "Could not obtain descriptor for Platform Managed OAuth Connection "
                                        + oauthConfig.getConnectionUri(),
                                    e);
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
    Consumer<ResourceOwnerOAuthContext> onUpdate = context -> updateOAuthParameters(unwrappedDelegate, callbackValues, context);
    oauthConfig.getDelegateGrantType().accept(new OAuthGrantTypeVisitor() {

      @Override
      public void visit(AuthorizationCodeGrantType grantType) {
        oauthStateFieldSetter.set(unwrappedDelegate, new PlatformAuthorizationCodeStateAdapter(dancer,
                                                                                               descriptor,
                                                                                               onUpdate));
      }

      @Override
      public void visit(ClientCredentialsGrantType grantType) {
        oauthStateFieldSetter.set(unwrappedDelegate, new PlatformClientCredentialsOAuthStateAdapter(dancer, onUpdate));
      }

      @Override
      public void visit(PlatformManagedOAuthGrantType grantType) {
        throw illegalDelegateException();
      }
    });

    onUpdate.accept(oauthHandler.getOAuthContext(oauthConfig));
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
  public String getResourceOwnerId() {
    return getContext().getResourceOwnerId();
  }

  @Override
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return ConnectionUtils.getRetryPolicyTemplate(getReconnectionConfig());
  }

  @Override
  public ConnectionProvider<C> getDelegate() {
    checkState(delegate != null, "ConnectionProvider has not been started yet");
    return delegate;
  }

  @Override
  public ConnectionManagementType getConnectionManagementType() {
    return oauthConfig.getDelegateConnectionProviderModel().getConnectionManagementType();
  }

  @Override
  public void onBorrow(C connection) {
    delegatePoolingListener.onBorrow(connection);
  }

  @Override
  public void onReturn(C connection) {
    delegatePoolingListener.onReturn(connection);
  }

  private PoolingListener<C> getDelegatePoolingListener() {
    if (unwrappedDelegate instanceof PoolingListener) {
      return new PoolingListener<C>() {

        @Override
        public void onBorrow(C connection) {
          ((PoolingListener) unwrappedDelegate).onBorrow(connection);
        }

        @Override
        public void onReturn(C connection) {
          ((PoolingListener) unwrappedDelegate).onReturn(connection);
        }
      };
    } else {
      return new PoolingListener<C>() {

        @Override
        public void onBorrow(C connection) {
          throw new UnsupportedOperationException("The delegate connection provider does not support Pooling");
        }

        @Override
        public void onReturn(C connection) {
          throw new UnsupportedOperationException("The delegate connection provider does not support Pooling");
        }
      };
    }
  }

}
