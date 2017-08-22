/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.loader.validation.ModelValidationUtils.validateConfigOverrideParametersNotAllowed;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.util.MuleExtensionUtils;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ExtensionModelValidator} which applies to {@link ExtensionModel}s which either contains {@link ConnectionProviderModel}s,
 * {@link OperationModel}s which require a connection or both.
 * <p>
 * This validator makes sure that:
 * <ul>
 * <li>All operations require the same type of connections</li>
 * <li>All the {@link ConnectionProvider}s return connections of the same type as expected by the {@link OperationModel}s</li>
 * <li>Transactional connections can not be produced by {@link CachedConnectionProvider}s</li>
 * </ul>
 *
 * @since 4.0
 */
public final class ConnectionProviderModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    Set<ConnectionProviderModel> globalConnectionProviders = new HashSet<>();
    Multimap<ConfigurationModel, ConnectionProviderModel> configLevelConnectionProviders = HashMultimap.create();

    new ExtensionWalker() {

      @Override
      public void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        if (owner instanceof ConfigurationModel) {
          configLevelConnectionProviders.put((ConfigurationModel) owner, model);
        } else {
          globalConnectionProviders.add(model);
        }

        validateConfigOverrideParametersNotAllowed(model, problemsReporter, "Connection");
      }
    }.walk(extensionModel);

    validateGlobalConnectionTypes(extensionModel, globalConnectionProviders, problemsReporter);
    validateConfigLevelConnectionTypes(configLevelConnectionProviders, problemsReporter);
  }

  private void validateGlobalConnectionTypes(ExtensionModel extensionModel,
                                             Set<ConnectionProviderModel> globalConnectionProviders,
                                             ProblemsReporter problemsReporter) {
    if (globalConnectionProviders.isEmpty()) {
      return;
    }

    for (ConnectionProviderModel connectionProviderModel : globalConnectionProviders) {
      final Class<?> connectionType = MuleExtensionUtils.getConnectionType(connectionProviderModel);

      new IdempotentExtensionWalker() {

        @Override
        protected void onOperation(OperationModel operationModel) {
          validateConnectionTypes(connectionProviderModel, operationModel, connectionType, problemsReporter);
        }

        @Override
        protected void onSource(SourceModel sourceModel) {
          validateConnectionTypes(connectionProviderModel, sourceModel, connectionType, problemsReporter);
        }
      }.walk(extensionModel);
    }
  }

  private void validateConfigLevelConnectionTypes(Multimap<ConfigurationModel, ConnectionProviderModel> configLevelConnectionProviders,
                                                  ProblemsReporter problemsReporter) {
    configLevelConnectionProviders.asMap().forEach((configModel, providerModels) -> {
      for (ConnectionProviderModel providerModel : providerModels) {
        Class<?> connectionType = MuleExtensionUtils.getConnectionType(providerModel);
        configModel.getOperationModels()
            .forEach(operationModel -> validateConnectionTypes(providerModel, operationModel, connectionType, problemsReporter));
      }
    });
  }

  private <T> Optional<Class<T>> getConnectionType(EnrichableModel model) {
    Optional<ConnectivityModelProperty> connectivityProperty = model.getModelProperty(ConnectivityModelProperty.class);
    if (!connectivityProperty.isPresent() && model instanceof ParameterizedModel) {
      connectivityProperty = ((ParameterizedModel) model).getAllParameterModels().stream()
          .map(p -> p.getModelProperty(ConnectivityModelProperty.class).orElse(null)).filter(p -> p != null).findFirst();
    }

    return connectivityProperty.map(property -> (Class<T>) property.getConnectionType());
  }

  private void validateConnectionTypes(ConnectionProviderModel providerModel,
                                       ComponentModel componentModel, Class<?> providerConnectionType,
                                       ProblemsReporter problemsReporter) {
    getConnectionType(componentModel).ifPresent(connectionType -> {
      if (!connectionType.isAssignableFrom(providerConnectionType)) {
        problemsReporter
            .addError(new Problem(providerModel,
                                  format("Component '%s' requires a connection of type '%s'. However, it also defines connection provider "
                                      + "'%s' which yields connections of incompatible type '%s'",
                                         componentModel.getName(), connectionType.getName(), providerModel.getName(),
                                         providerConnectionType.getName())));
      }
    });

  }
}
