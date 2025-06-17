/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.MinMuleVersionUtils.declarerWithMmv;

import static java.util.Optional.of;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.property.NoImplicitModelProperty;
import org.mule.runtime.extension.api.loader.parser.ConfigurationModelParser;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;

/**
 * Helper class for declaring configurations through a {@link DefaultExtensionModelLoaderDelegate}
 *
 * @since 4.0
 */
final class ConfigModelLoaderDelegate extends AbstractComponentModelLoaderDelegate {

  ConfigModelLoaderDelegate(DefaultExtensionModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareConfigurations(ExtensionDeclarer declarer, ExtensionModelParser extensionModelParser,
                             ExtensionLoadingContext context) {
    for (ConfigurationModelParser configParser : extensionModelParser.getConfigurationParsers()) {

      String configName = resolveConfigName(configParser);
      ConfigurationDeclarer configurationDeclarer = declarer.withConfig(configName)
          .describedAs(resolveConfigDescription(configParser, configName))
          .withModelProperty(new ConfigurationFactoryModelProperty(configParser.getConfigurationFactory()));

      if (configParser.isForceNoImplicit()) {
        configurationDeclarer.withModelProperty(new NoImplicitModelProperty());
      }

      configParser.getDeprecationModel().ifPresent(dm -> configurationDeclarer.getDeclaration().withDeprecation(dm));
      configParser.getDisplayModel().ifPresent(d -> configurationDeclarer.getDeclaration().setDisplayModel(d));
      configParser.getExternalLibraryModels().forEach(configurationDeclarer::withExternalLibrary);
      configParser.getAdditionalModelProperties().forEach(configurationDeclarer::withModelProperty);
      if (context.isResolveMinMuleVersion()) {
        configParser.getResolvedMinMuleVersion().ifPresent(resolvedMMV -> declarerWithMmv(configurationDeclarer, resolvedMMV));
      }
      loader.getParameterModelsLoaderDelegate().declare(configurationDeclarer, configParser.getParameterGroupParsers(), context);

      getOperationLoaderDelegate().declareOperations(declarer, extensionModelParser.getDevelopmentFramework(),
                                                     configurationDeclarer, configParser.getOperationParsers(), context);
      getSourceModelLoaderDelegate().declareMessageSources(declarer, configurationDeclarer,
                                                           configParser.getSourceModelParsers(),
                                                           context);
      getFunctionModelLoaderDelegate().declareFunctions(declarer, configParser.getFunctionModelParsers(), context);
      getConnectionProviderModelLoaderDelegate().declareConnectionProviders(configurationDeclarer,
                                                                            configParser.getConnectionProviderModelParsers(),
                                                                            context);

      getStereotypeModelLoaderDelegate().addStereotypes(configParser,
                                                        configurationDeclarer,
                                                        of(() -> getStereotypeModelLoaderDelegate()
                                                            .getDefaultConfigStereotype(configParser.getName())));
    }
  }

  private String resolveConfigName(ConfigurationModelParser parser) {
    return isBlank(parser.getName()) ? DEFAULT_CONFIG_NAME : parser.getName();
  }

  private String resolveConfigDescription(ConfigurationModelParser parser, String configName) {
    String description = parser.getDescription();
    if (isBlank(description) && DEFAULT_CONFIG_NAME.equals(configName)) {
      description = DEFAULT_CONFIG_DESCRIPTION;
    }

    return description;
  }
}
