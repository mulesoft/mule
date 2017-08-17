/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapServiceProviderDeclarer.TRANSPORT_PARAM;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.internal.connection.ErrorTypeHandlerConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.soap.api.runtime.connection.transport.DefaultHttpMessageDispatcherProvider;
import org.mule.runtime.soap.api.client.SoapClient;

/**
 * Implementation of {@link ConnectionProviderObjectBuilder} which produces instances of {@link ForwardingSoapClientConnectionProvider}.
 *
 * @since 4.0
 */
public final class SoapConnectionProviderObjectBuilder extends ConnectionProviderObjectBuilder<SoapClient> {

  private final DefaultResolverSetBasedObjectBuilder<SoapServiceProvider> objectBuilder;

  public SoapConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                             ResolverSet resolverSet,
                                             PoolingProfile poolingProfile,
                                             ReconnectionConfig reconnectionConfig,
                                             ExtensionModel extensionModel,
                                             MuleContext muleContext) {
    super(providerModel, getServiceProviderType(providerModel), resolverSet, poolingProfile,
          reconnectionConfig, extensionModel, muleContext);
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
  public Pair<ConnectionProvider<SoapClient>, ResolverSetResult> build(ResolverSetResult result) throws MuleException {
    SoapServiceProvider serviceProvider = objectBuilder.build(result);
    MessageDispatcherProvider<? extends MessageDispatcher> transport = getCustomTransport(result);
    ConnectionProvider<ForwardingSoapClient> provider =
        new ForwardingSoapClientConnectionProvider(serviceProvider, transport, muleContext);
    provider = new ReconnectableConnectionProviderWrapper<>(provider, reconnectionConfig);
    provider = new ErrorTypeHandlerConnectionProviderWrapper<>(provider, extensionModel, reconnectionConfig, muleContext);
    return new Pair(provider, result);
  }

  private MessageDispatcherProvider<MessageDispatcher> getCustomTransport(ResolverSetResult resultSet) {
    MessageDispatcherProvider customTransport = (MessageDispatcherProvider) resultSet.get(TRANSPORT_PARAM);
    return customTransport != null ? customTransport : new DefaultHttpMessageDispatcherProvider();
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
