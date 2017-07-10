/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Optional.of;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.extension.api.values.ValueResolvingException.UNKNOWN;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_METADATA_SERVICE;
import static org.mule.runtime.extension.api.metadata.NullMetadataResolver.NULL_CATEGORY_NAME;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getConnectionProviderModel;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getAllConnectionProviders;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getMetadataResolverFactory;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyProvider;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataKeysContainerBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.values.ConfigurationParameterValueProvider;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBasedParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.value.ValueProviderMediator;

import java.util.List;
import java.util.Set;

/**
 * Adds the capability to expose tooling focused capabilities associated with the {@link StaticConfigurationProvider}'s
 * components.
 * So sar the capabilities are:
 * <ul>
 * <li>{@link MetadataKeyProvider}, to resolve {@link MetadataKey metadata keys} associated to a configuration</li>
 * <li>{@link ConfigurationParameterValueProvider}, to resolve {@link Value values} associated to a configuration and
 * their related connection</li>
 * </ul>
 *
 * @since 4.0
 */
public final class ConfigurationProviderToolingAdapter extends StaticConfigurationProvider
    implements MetadataKeyProvider, ConfigurationParameterValueProvider {

  private final MuleMetadataService metadataService;
  protected final ConnectionManager connectionManager;
  private final ConfigurationInstance configuration;

  ConfigurationProviderToolingAdapter(String name,
                                      ExtensionModel extensionModel,
                                      ConfigurationModel configurationModel,
                                      ConfigurationInstance configuration,
                                      MuleContext muleContext) {
    super(name, extensionModel, configurationModel, configuration, muleContext);
    this.configuration = configuration;
    this.metadataService = muleContext.getRegistry().get(OBJECT_METADATA_SERVICE);
    this.connectionManager = muleContext.getRegistry().get(OBJECT_CONNECTION_MANAGER);
  }

  /**
   * {@inheritDoc}
   */
  public MetadataResult<MetadataKeysContainer> getMetadataKeys() throws MetadataResolvingException {

    MetadataKeysContainerBuilder keysBuilder = MetadataKeysContainerBuilder.getInstance();
    try {
      MetadataContext metadataContext = getMetadataContext();
      addComponentKeys(getConfigurationModel().getOperationModels(), metadataContext, keysBuilder);
      addComponentKeys(getConfigurationModel().getSourceModels(), metadataContext, keysBuilder);
      metadataContext.dispose();
    } catch (Exception e) {
      return failure(newFailure(e).onKeys());
    }
    return success(keysBuilder.build());
  }

  private void addComponentKeys(List<? extends ComponentModel> components, MetadataContext metadataContext,
                                MetadataKeysContainerBuilder keysBuilder)
      throws MetadataResolvingException, ConnectionException {
    for (ComponentModel component : components) {
      TypeKeysResolver keysResolver = getMetadataResolverFactory(component).getKeyResolver();

      String categoryName = keysResolver.getCategoryName();
      if (!NULL_CATEGORY_NAME.equals(categoryName) && !keysBuilder.containsCategory(categoryName)) {
        keysBuilder.add(categoryName, keysResolver.getKeys(metadataContext));
      }
    }
  }

  private MetadataContext getMetadataContext() throws MetadataResolvingException, ConnectionException {
    Event fakeEvent = getInitialiserEvent(muleContext);
    return new DefaultMetadataContext(of(get(fakeEvent)),
                                      connectionManager,
                                      metadataService.getMetadataCache(getName()),
                                      ExtensionsTypeLoaderFactory.getDefault()
                                          .createTypeLoader(getClassLoader(getExtensionModel())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Value> getConfigValues(String parameterName) throws ValueResolvingException {
    Reference<Set<Value>> options = new Reference<>();
    ConfigurationModel configurationModel = getConfigurationModel();
    ValueProviderMediator<ConfigurationModel> valueProviderMediator =
        new ValueProviderMediator<>(configurationModel, muleContext);
    options
        .set(valueProviderMediator.getValues(parameterName,
                                             getParameterValueResolver(configuration.getValue(), configurationModel)));
    return options.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Value> getConnectionValues(String parameterName) throws ValueResolvingException {
    Reference<Set<Value>> options = new Reference<>();

    ConnectionProvider<?> connectionProvider = configuration.getConnectionProvider()
        .orElseThrow(() -> new ValueResolvingException("Unable to obtain the Connection Provider Instance", UNKNOWN));

    ConnectionProvider unwrap = unwrap(connectionProvider);
    ConnectionProviderModel connectionProviderModel =
        getConnectionProviderModel(unwrap.getClass(), getAllConnectionProviders(getExtensionModel(), getConfigurationModel()))
            .orElseThrow(() -> new ValueResolvingException("Internal error. Unable to obtain the Connection Provider Model",
                                                           UNKNOWN));

    ValueProviderMediator<ConnectionProviderModel> valueProviderMediator =
        new ValueProviderMediator<>(connectionProviderModel, muleContext);

    options.set(valueProviderMediator.getValues(parameterName, getParameterValueResolver(unwrap, connectionProviderModel)));

    return options.get();
  }

  private ParameterValueResolver getParameterValueResolver(Object object, ParameterizedModel configurationModel) {
    return new ObjectBasedParameterValueResolver(object, configurationModel);
  }

  private <C> ConnectionProvider<C> unwrap(ConnectionProvider<C> connectionProvider) {
    return connectionProvider instanceof ConnectionProviderWrapper
        ? unwrap(((ConnectionProviderWrapper<C>) connectionProvider).getDelegate()) : connectionProvider;
  }
}
