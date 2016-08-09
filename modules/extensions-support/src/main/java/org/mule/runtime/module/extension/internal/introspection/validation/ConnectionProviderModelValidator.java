/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.ExtensionWalker;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.connection.HasConnectionProviderModels;
import org.mule.runtime.extension.api.introspection.connection.RuntimeConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.module.extension.internal.model.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentExtensionWalker;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sun.org.apache.xpath.internal.ExtensionsProvider;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ModelValidator} which applies to {@link ExtensionModel}s which either contains {@link ConnectionProviderModel}s,
 * {@link OperationModel}s which require a connection or both.
 * <p>
 * This validator makes sure that:
 * <ul>
 * <li>All operations require the same type of connections</li>
 * <li>All the {@link ExtensionsProvider}s are compatible with all the {@link ConfigurationModel}s in the extension</li>
 * <li>All the {@link ExtensionsProvider}s return connections of the same type as expected by the {@link OperationModel}s</li>
 * </ul>
 *
 * @since 4.0
 */
public final class ConnectionProviderModelValidator implements ModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {
    Set<ConnectionProviderModel> globalConnectionProviders = new HashSet<>();
    Multimap<ConfigurationModel, ConnectionProviderModel> configLevelConnectionProviders = HashMultimap.create();

    new ExtensionWalker() {

      @Override
      public void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        validateTransactions(extensionModel, (RuntimeConnectionProviderModel) model);
        if (owner instanceof ConfigurationModel) {
          configLevelConnectionProviders.put((ConfigurationModel) owner, model);
        } else {
          globalConnectionProviders.add(model);
        }
      }
    }.walk(extensionModel);

    validateGlobalConnectionTypes(extensionModel, globalConnectionProviders);
    validateConfigLevelConnectionTypes(extensionModel, configLevelConnectionProviders);
  }

  private void validateTransactions(ExtensionModel extensionModel, RuntimeConnectionProviderModel connectionProviderModel) {
    Class<ConnectionProvider> providerType = (Class<ConnectionProvider>) connectionProviderModel
        .getModelProperty(ImplementingTypeModelProperty.class).map(ImplementingTypeModelProperty::getType).orElse(null);

    if (providerType != null && CachedConnectionProvider.class.isAssignableFrom(providerType)
        && TransactionalConnection.class.isAssignableFrom(connectionProviderModel.getConnectionType())) {
      throw new IllegalConnectionProviderModelDefinitionException(format("Extension '%s' contains a cached connection provider of name '%s' which provides connections of "
          + "transactional type '%s'. Transactional connections cannot be produced by cached providers, since the "
          + "same connection cannot join two different transactions at once", extensionModel.getName(),
                                                                         connectionProviderModel.getName(),
                                                                         connectionProviderModel.getConnectionType().getName()));
    }
  }

  private void validateGlobalConnectionTypes(ExtensionModel extensionModel,
                                             Set<ConnectionProviderModel> globalConnectionProviders) {
    if (isEmpty(globalConnectionProviders)) {
      return;
    }

    for (ConnectionProviderModel model : globalConnectionProviders) {
      RuntimeConnectionProviderModel connectionProviderModel = (RuntimeConnectionProviderModel) model;
      final Class<?> connectionType = connectionProviderModel.getConnectionType();

      new IdempotentExtensionWalker() {

        @Override
        protected void onOperation(OperationModel operationModel) {
          validateConnectionTypes(extensionModel, connectionProviderModel, operationModel, connectionType);
        }

        @Override
        protected void onSource(SourceModel sourceModel) {
          validateConnectionTypes(extensionModel, connectionProviderModel, sourceModel, connectionType);
        }
      }.walk(extensionModel);

    }
  }

  private void validateConfigLevelConnectionTypes(ExtensionModel extensionModel,
                                                  Multimap<ConfigurationModel, ConnectionProviderModel> configLevelConnectionProviders) {
    configLevelConnectionProviders.asMap().forEach((configModel, providerModels) -> {
      for (ConnectionProviderModel providerModel : providerModels) {
        Class<?> connectionType = ((RuntimeConnectionProviderModel) providerModel).getConnectionType();
        configModel.getOperationModels()
            .forEach(operationModel -> validateConnectionTypes(extensionModel, providerModel, operationModel, connectionType));
      }
    });
  }

  private <T> Optional<Class<T>> getConnectionType(EnrichableModel model) {
    Optional<ConnectivityModelProperty> connectivityProperty = model.getModelProperty(ConnectivityModelProperty.class);
    if (!connectivityProperty.isPresent() && model instanceof ParameterizedModel) {
      connectivityProperty = ((ParameterizedModel) model).getParameterModels().stream()
          .map(p -> p.getModelProperty(ConnectivityModelProperty.class).orElse(null)).filter(p -> p != null).findFirst();
    }

    return connectivityProperty.map(property -> getType(property.getConnectionType()));
  }

  private void validateConnectionTypes(ExtensionModel extensionModel, ConnectionProviderModel providerModel,
                                       ComponentModel componentModel, Class<?> providerConnectionType) {
    getConnectionType(componentModel).ifPresent(connectionType -> {
      if (!connectionType.isAssignableFrom(providerConnectionType)) {
        throw new IllegalConnectionProviderModelDefinitionException(format("Extension '%s' defines component '%s' which requires a connection of type '%s'. However, it also defines connection provider "
            + "'%s' which yields connections of incompatible type '%s'", extensionModel.getName(), componentModel.getName(),
                                                                           connectionType.getName(), providerModel.getName(),
                                                                           providerConnectionType.getName()));
      }
    });

  }
}
