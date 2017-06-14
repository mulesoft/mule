/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.ErrorTypeHandlerConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.PoolingConnectionProviderWrapper;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.transport.DefaultHttpMessageDispatcherProvider;
import org.mule.runtime.soap.api.client.SoapClient;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapServiceProviderDeclarer.CUSTOM_TRANSPORT;

/**
 * Implementation of {@link ConnectionProviderObjectBuilder} which produces instances of {@link ForwardingSoapClientConnectionProvider}.
 *
 * @since 4.0
 */
public final class SoapConnectionProviderObjectBuilder extends ConnectionProviderObjectBuilder<SoapClient> {

  private final DefaultResolverSetBasedObjectBuilder<SoapServiceProvider> objectBuilder;

  /**
   * Creates a new instances which produces instances based on the given {@code providerModel} and {@code resolverSet}
   *
   * @param providerModel     the {@link ConnectionProviderModel} which describes the instances to be produced
   * @param resolverSet       a {@link ResolverSet} to populate the values
   * @param connectionManager a {@link ConnectionManagerAdapter} to obtain the default {@link RetryPolicyTemplate} in case of none
   *                          is provided
   */
  public SoapConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                             PoolingProfile poolingProfile, boolean disableValidation,
                                             RetryPolicyTemplate retryPolicyTemplate, ConnectionManagerAdapter connectionManager,
                                             ExtensionModel extensionModel, MuleContext muleContext) {
    super(providerModel, getServiceProviderType(providerModel), resolverSet, poolingProfile,
          disableValidation, retryPolicyTemplate, connectionManager, extensionModel, muleContext);
    objectBuilder = new DefaultResolverSetBasedObjectBuilder<>(getServiceProviderType(providerModel), resolverSet);
  }

  /**
   * Build a new {@link ForwardingSoapClientConnectionProvider} based on a {@link SoapServiceProvider} instance.
   *
   * @param result the {@link ResolverSetResult} with the values for the {@link SoapServiceProvider} instance.
   * @return a wrapped {@link ForwardingSoapClientConnectionProvider} with error handling and polling mechanisms.
   * @throws MuleException
   */
  @Override
  public ConnectionProvider build(ResolverSetResult result) throws MuleException {
    SoapServiceProvider serviceProvider = objectBuilder.build(result);
    MessageDispatcherProvider<? extends MessageDispatcher> transport = getCustomTransport(result);
    muleContext.getInjector().inject(serviceProvider);
    ConnectionProvider<ForwardingSoapClient> provider = new ForwardingSoapClientConnectionProvider(serviceProvider, transport);
    provider = new PoolingConnectionProviderWrapper<>(provider, poolingProfile, disableValidation, retryPolicyTemplate);
    provider = new ErrorTypeHandlerConnectionProviderWrapper<>(provider, muleContext, extensionModel, retryPolicyTemplate);
    return provider;
  }

  private MessageDispatcherProvider<? extends MessageDispatcher> getCustomTransport(ResolverSetResult resultSet)
      throws MuleException {
    MessageDispatcherProvider customTransport = (MessageDispatcherProvider) resultSet.get(CUSTOM_TRANSPORT);
    MessageDispatcherProvider transport = customTransport != null ? customTransport : new DefaultHttpMessageDispatcherProvider();
    Injector injector = muleContext.getInjector();
    injector.inject(transport);
    return transport;
  }

  /**
   * @return a {@link SoapServiceProvider} implementation {@link Class} for a given {@link ConnectionProviderModel}.
   */
  private static Class<SoapServiceProvider> getServiceProviderType(ConnectionProviderModel model) {
    return model.getModelProperty(ImplementingTypeModelProperty.class)
        .map(prop -> (Class<SoapServiceProvider>) prop.getType())
        .orElseThrow(() -> new IllegalStateException(format("No %s was defined in connection provider [%s]",
                                                            ImplementingTypeModelProperty.class.getSimpleName(),
                                                            model.getName())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return resolverSet.isDynamic();
  }
}
