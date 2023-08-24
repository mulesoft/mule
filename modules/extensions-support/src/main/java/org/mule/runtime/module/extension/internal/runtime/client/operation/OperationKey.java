/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.operation;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Optional.of;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
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

  private final String configName;
  private final ExtensionModel extensionModel;
  private final OperationModel operationModel;
  private final ExtensionManager extensionManager;

  private Optional<ConfigurationProvider> configurationProvider;

  public OperationKey(ExtensionModel extensionModel,
                      OperationModel operationModel,
                      String configName,
                      ExtensionManager extensionManager) {
    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    this.configName = configName;
    this.extensionManager = extensionManager;
  }

  public Optional<ConfigurationProvider> getConfigurationProvider(CoreEvent contextEvent) {
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
                throw new IllegalArgumentException(format(
                                                          "A config of the '%s' extension was expected but one from '%s' was parameterized instead",
                                                          extensionModel.getName(),
                                                          configurationProvider.getExtensionModel().getName()));
              }
              return configurationProvider;
            })
            .orElseThrow(() -> new MuleRuntimeException(
                                                        createStaticMessage("No configuration [" + configName + "] found"))));
      } else {
        configurationProvider = extensionManager.getConfigurationProvider(extensionModel, operationModel, contextEvent);
      }
    }

    return configurationProvider;
  }


  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  public OperationModel getOperationModel() {
    return operationModel;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof OperationKey) {
      OperationKey that = (OperationKey) o;
      return Objects.equals(extensionModel.getName(), that.extensionModel.getName()) &&
          Objects.equals(configName, that.configName) &&
          Objects.equals(operationModel.getName(), that.operationModel.getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hash(extensionModel.getName(), operationModel.getName(), configName);
  }

  @Override
  public String toString() {
    return format("[Extension: %s; Operation: %s, ConfigName: %s", extensionModel.getName(), operationModel.getName(),
                  configName);
  }
}
