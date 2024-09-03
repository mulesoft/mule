/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.addSemanticTerms;

import static java.util.Optional.of;

import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConnectionProviderDeclarer;
import org.mule.runtime.extension.api.property.ExcludeFromConnectivitySchemaModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for declaring connection providers through a {@link DefaultExtensionModelLoaderDelegate}
 *
 * @since 4.0
 */
final class ConnectionProviderModelLoaderDelegate extends AbstractComponentModelLoaderDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProviderModelLoaderDelegate.class);

  private final Map<ConnectionProviderModelParser, ConnectionProviderDeclarer> connectionProviderDeclarers = new HashMap<>();

  ConnectionProviderModelLoaderDelegate(DefaultExtensionModelLoaderDelegate loader) {
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
          .supportsConnectivityTesting(parser.supportsConnectivityTesting());

      ConnectionProviderDeclaration connectionProviderDeclaration = providerDeclarer.getDeclaration();
      parser.getDeprecationModel().ifPresent(connectionProviderDeclaration::withDeprecation);
      parser.getDisplayModel().ifPresent(connectionProviderDeclaration::setDisplayModel);
      if (parser.mustResolveMinMuleVersion()) {
        parser.getResolvedMinMuleVersion().ifPresent(resolvedMMV -> {
          connectionProviderDeclaration.withMinMuleVersion(resolvedMMV.getMinMuleVersion());
          LOGGER.debug(resolvedMMV.getReason());
        });
      }

      parser.getConnectionProviderFactoryModelProperty().ifPresent(providerDeclarer::withModelProperty);

      if (parser.isExcludedFromConnectivitySchema()) {
        providerDeclarer.withModelProperty(new ExcludeFromConnectivitySchemaModelProperty());
      }

      parser.getExternalLibraryModels().forEach(providerDeclarer::withExternalLibrary);
      parser.getOAuthModelProperty().ifPresent(providerDeclarer::withModelProperty);

      loader.getParameterModelsLoaderDelegate().declare(providerDeclarer, parser.getParameterGroupModelParsers());
      parser.getAdditionalModelProperties().forEach(providerDeclarer::withModelProperty);
      addSemanticTerms(providerDeclarer.getDeclaration(), parser);
      getStereotypeModelLoaderDelegate().addStereotypes(
                                                        parser,
                                                        providerDeclarer,
                                                        of(() -> getStereotypeModelLoaderDelegate()
                                                            .getDefaultConnectionProviderStereotype(parser.getName())));

      connectionProviderDeclarers.put(parser, providerDeclarer);
    }
  }
}
