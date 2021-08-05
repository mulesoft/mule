/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConnectionProviderDeclarer;
import org.mule.runtime.connectivity.api.platform.schema.extension.ExcludeFromConnectivitySchemaModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for declaring connection providers through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class ConnectionProviderModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private final Map<ConnectionProviderModelParser, ConnectionProviderDeclarer> connectionProviderDeclarers = new HashMap<>();

  ConnectionProviderModelLoaderDelegate(DefaultJavaModelLoaderDelegate loader) {
    super(loader);
  }

  void declareConnectionProviders(HasConnectionProviderDeclarer declarer, List<ConnectionProviderModelParser> parsers) {
    for (ConnectionProviderModelParser parser : parsers) {

      ConnectionProviderDeclarer providerDeclarer = connectionProviderDeclarers.get(parser);
      if (providerDeclarer != null) {
        declarer.withConnectionProvider(providerDeclarer);
        continue;
      }

      providerDeclarer = declarer.withConnectionProvider(parser.getName())
          .describedAs(parser.getDescription())
          .withConnectionManagementType(parser.getConnectionManagementType())
          .supportsConnectivityTesting(parser.supportsConnectivityTesting())
          .withModelProperty(parser.getConnectionProviderFactoryModelProperty());

      if (parser.isExcludedFromConnectivitySchema()) {
        providerDeclarer.withModelProperty(new ExcludeFromConnectivitySchemaModelProperty());
      }

      parser.getExternalLibraryModels().forEach(providerDeclarer::withExternalLibrary);
      parser.getOAuthModelProperty().ifPresent(providerDeclarer::withModelProperty);

      loader.getParameterModelsLoaderDelegate().declare(providerDeclarer, parser.getParameterGroupModelParsers());
      parser.getAdditionalModelProperties().forEach(providerDeclarer::withModelProperty);
      connectionProviderDeclarers.put(parser, providerDeclarer);
    }
  }
}
