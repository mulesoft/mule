/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.operation;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.func.CheckedBiFunction;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * Identifies an extension operation and its referenced {@link ConfigurationProvider} to be executed in the context of a
 * {@link ExtensionsClient}
 *
 * @since 4.5.0
 */
public class OperationKey {

  private final String extensionName;
  private final String configName;
  private final String operationName;
  private final Once.RunOnce initializer;
  private final ExtensionManager extensionManager;

  private ExtensionModel extensionModel;
  private Optional<ConfigurationProvider> configurationProvider;
  private OperationModel operationModel;

  public OperationKey(String extensionName,
                      String configName,
                      String operationName,
                      CheckedFunction<String, ExtensionModel> extensionResolver,
                      CheckedBiFunction<ExtensionModel, String, OperationModel> operationResolver,
                      ExtensionManager extensionManager) {
    this.extensionName = extensionName;
    this.configName = configName;
    this.operationName = operationName;
    this.extensionManager = extensionManager;

    initializer = Once.of(() -> {
      extensionModel = extensionResolver.apply(extensionName);
      operationModel = operationResolver.apply(extensionModel, operationName);
    });
  }

  public Optional<ConfigurationProvider> getConfigurationProvider(CoreEvent contextEvent) {
    initialize();

    if (configurationProvider != null) {
      return configurationProvider;
    }

    synchronized (this) {
      if (configurationProvider != null) {
        return configurationProvider;
      }

      if (configName != null) {
        configurationProvider = of(extensionManager.getConfigurationProvider(configName)
            .map(configurationProvider -> {
              if (configurationProvider.getExtensionModel() != extensionModel) {
                throw new IllegalArgumentException(format("A config of the '%s' extension was expected but one from '%s' was parameterized instead",
                    extensionModel.getName(), configurationProvider.getExtensionModel().getName()));
              }
              return configurationProvider;
            })
            .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No configuration [" + configName + "] found"))));
      } else {
        configurationProvider = extensionManager.getConfigurationProvider(extensionModel, operationModel,contextEvent);
      }
    }

    return configurationProvider;
  }

  private void initialize() {
    initializer.runOnce();
  }

  public ExtensionModel getExtensionModel() {
    initialize();
    return extensionModel;
  }

  public OperationModel getOperationModel() {
    initialize();
    return operationModel;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof OperationKey) {
      OperationKey that = (OperationKey) o;
      return Objects.equals(extensionName, that.extensionName) &&
          Objects.equals(configName, that.configName) &&
          Objects.equals(operationName, that.operationName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hash(extensionName, operationName, configName);
  }

  @Override
  public String toString() {
    return format("[Extension: %s; Operation: %s, ConfigName: %s", extensionName, operationName, configName);
  }
}
