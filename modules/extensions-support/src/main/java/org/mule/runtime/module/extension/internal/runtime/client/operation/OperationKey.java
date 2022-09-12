/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.operation;

import static java.lang.String.format;
import static java.util.Objects.hash;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
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

  private final ExtensionModel extensionModel;
  private final Optional<ConfigurationProvider> configurationProvider;
  private final OperationModel operationModel;
  private final String configName;

  public OperationKey(ExtensionModel extensionModel,
                      Optional<ConfigurationProvider> configurationProvider,
                      OperationModel operationModel) {
    this.extensionModel = extensionModel;
    this.configurationProvider = configurationProvider;
    this.operationModel = operationModel;
    configName = configurationProvider.map(ConfigurationProvider::getName).orElse(null);
  }

  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  public Optional<ConfigurationProvider> getConfigurationProvider() {
    return configurationProvider;
  }

  public OperationModel getOperationModel() {
    return operationModel;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof OperationKey) {
      OperationKey that = (OperationKey) o;
      return Objects.equals(extensionModel.getName(), that.extensionModel.getName()) &&
          Objects.equals(operationModel.getName(), that.operationModel.getName()) &&
          Objects.equals(configName, that.configName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hash(extensionModel.getName(), operationModel.getName(), configName);
  }

  @Override
  public String toString() {
    return format("[Extension: %s; Operation: %s, ConfigName: %s",
                  extensionModel.getName(), operationModel.getName(), configName);
  }
}
