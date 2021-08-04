/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;

import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.property.NoImplicitModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ComponentElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConfigurationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;

/**
 * Helper class for declaring configurations through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class ConfigModelLoaderDelegate extends AbstractModelLoaderDelegate {

  ConfigModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareConfigurations(ExtensionDeclarer declarer,
                             ExtensionModelParser extensionModelParser,
                             ExtensionLoadingContext context) {
    extensionModelParser.getConfigurationParsers().forEach(configParser -> declareConfiguration(declarer, configParser, context));
  }

  private void declareConfiguration(ExtensionDeclarer declarer,
                                    ConfigurationModelParser configParser,
                                    ExtensionLoadingContext loadingContext) {

    ConfigurationDeclarer configurationDeclarer = declarer.withConfig(configParser.getName())
        .describedAs(configParser.getDescription())
        .withModelProperty(configParser.getConfigurationFactoryModelProperty());

    if (configParser.isForceNoExplicit()) {
      configurationDeclarer.withModelProperty(new NoImplicitModelProperty());
    }

    configParser.getExternalLibraryModels().forEach(configurationDeclarer::withExternalLibrary);
    configParser.getAdditionalModelProperties().forEach(configurationDeclarer::withModelProperty);

    loader.getParameterModelsLoaderDelegate().declare(configurationDeclarer, configParser.getParameterGroupParsers());

    getOperationLoaderDelegate().declareOperations(declarer, configurationDeclarer, configParser.getOperationParsers());
    getSourceModelLoaderDelegate().declareMessageSources(declarer, configurationDeclarer, configParser.getSourceModelParsers());
    getFunctionModelLoaderDelegate().declareFunctions(declarer, configParser.getFunctionModelParsers());
    getConnectionProviderModelLoaderDelegate().declareConnectionProviders(configurationDeclarer, configType);
  }


}
