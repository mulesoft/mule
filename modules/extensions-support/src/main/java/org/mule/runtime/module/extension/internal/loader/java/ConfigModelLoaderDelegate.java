/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.util.FunctionalUtils.ifPresent;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;

import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;
import org.mule.runtime.api.meta.model.declaration.fluent.HasStereotypeDeclarer;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.property.NoImplicitModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConfigurationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.CustomStereotypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.StereotypeResolver;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper class for declaring configurations through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class ConfigModelLoaderDelegate extends AbstractModelLoaderDelegate {

  ConfigModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareConfigurations(ExtensionDeclarer declarer, ExtensionModelParser extensionModelParser) {
    for (ConfigurationModelParser configParser : extensionModelParser.getConfigurationParsers()) {

      String configName = resolveConfigName(configParser);
      ConfigurationDeclarer configurationDeclarer = declarer.withConfig(configName)
          .describedAs(resolveConfigDescription(configParser, configName))
          .withModelProperty(configParser.getConfigurationFactoryModelProperty());

      if (configParser.isForceNoExplicit()) {
        configurationDeclarer.withModelProperty(new NoImplicitModelProperty());
      }

      configParser.getDeprecationModel().ifPresent(dm -> configurationDeclarer.getDeclaration().withDeprecation(dm));
      configParser.getExternalLibraryModels().forEach(configurationDeclarer::withExternalLibrary);
      configParser.getAdditionalModelProperties().forEach(configurationDeclarer::withModelProperty);

      loader.getParameterModelsLoaderDelegate().declare(configurationDeclarer, configParser.getParameterGroupParsers());

      getOperationLoaderDelegate().declareOperations(declarer, configurationDeclarer, configParser.getOperationParsers());
      getSourceModelLoaderDelegate().declareMessageSources(declarer, configurationDeclarer, configParser.getSourceModelParsers());
      getFunctionModelLoaderDelegate().declareFunctions(declarer, configParser.getFunctionModelParsers());
      getConnectionProviderModelLoaderDelegate().declareConnectionProviders(
                                                                            configurationDeclarer,
                                                                            configParser.getConnectionProviderModelParsers());
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

  private void <T extends HasStereotypeDeclarer & HasModelProperties> parseStereotype(StereotypeModelParser parser,
                                                                                      T declarer,
                                                                                      Optional<Supplier<StereotypeModel>> fallback) {

    StereotypeModelParser.ParsedStereotype stereotype = parser.getParsedStereotype();
    if (stereotype.isValidator() || stereotype.getStereotypeModel().isPresent()) {
      declarer.withModelProperty(new CustomStereotypeModelProperty());
    }

    StereotypeModel model = stereotype.getStereotypeModel()
        .orElseGet(() -> {
          if (stereotype.isValidator()) {
            return getStereotypeModelLoaderDelegate().getValidatorStereotype();
          } else {
            return fallback.map(Supplier::get).orElse(null);
          }
        });

    if (model != null) {
      declarer.withStereotype(model);
    }
    if (stereotype.isValidator()) {

      declarer.withStereotype(getStereotypeModelLoaderDelegate().getValidatorStereotype());
    }
    final StereotypeModel defaultStereotype = createStereotype(config.getName(), CONFIG);
    ifPresent(config.getModelProperty(ExtensionTypeDescriptorModelProperty.class)
            .map(ExtensionTypeDescriptorModelProperty::getType),
        type -> resolveStereotype(type, config, defaultStereotype),
        () -> config.withStereotype(defaultStereotype));
    componentConfigs = populateComponentConfigsMap(config);

  }
}
