/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ComponentElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;

import java.util.List;
import java.util.Optional;

/**
 * Helper class for declaring configurations through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class ConfigModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private static final String CONFIGURATION = "Configuration";

  ConfigModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareConfigurations(ExtensionDeclarer declaration, ExtensionElement extensionElement) {
    List<ConfigurationElement> configurations = extensionElement.getConfigurations();
    if (configurations.isEmpty()) {
      declareConfiguration(declaration, extensionElement, extensionElement);
    } else {
      for (ConfigurationElement configuration : configurations) {
        declareConfiguration(declaration, extensionElement, configuration);
      }
    }
  }

  private void declareConfiguration(ExtensionDeclarer declarer, ExtensionElement extensionType, ComponentElement configType) {
    checkConfigurationIsNotAnOperation(configType.getDeclaringClass());
    ConfigurationDeclarer configurationDeclarer;

    Optional<Configuration> configurationAnnotation = configType.getAnnotation(Configuration.class);
    if (configurationAnnotation.isPresent()) {
      final Configuration configuration = configurationAnnotation.get();
      configurationDeclarer = declarer.withConfig(configuration.name());
    } else {
      configurationDeclarer =
          declarer.withConfig(DEFAULT_CONFIG_NAME).describedAs(DEFAULT_CONFIG_DESCRIPTION);
    }

    TypeAwareConfigurationFactory typeAwareConfigurationFactory =
        new TypeAwareConfigurationFactory(configType.getDeclaringClass(), extensionType.getDeclaringClass().getClassLoader());
    configurationDeclarer
        .withModelProperty(new ConfigurationFactoryModelProperty(typeAwareConfigurationFactory))
        .withModelProperty(new ImplementingTypeModelProperty(configType.getDeclaringClass()));

    loader.parseExternalLibs(configType, configurationDeclarer);
    ParameterDeclarationContext context = new ParameterDeclarationContext(CONFIGURATION, configurationDeclarer.getDeclaration());
    loader.getFieldParametersLoader().declare(configurationDeclarer, configType.getParameters(), context);

    getOperationLoaderDelegate().declareOperations(declarer, configurationDeclarer, configType);
    getSourceModelLoaderDelegate().declareMessageSources(declarer, configurationDeclarer, configType);
    getFunctionModelLoaderDelegate().declareFunctions(declarer, configurationDeclarer, configType);
    getConnectionProviderModelLoaderDelegate().declareConnectionProviders(configurationDeclarer, configType);
  }

  private void checkConfigurationIsNotAnOperation(Class<?> configurationType) {
    Class<?>[] operationClasses = loader.getOperationClasses(getExtensionType());
    for (Class<?> operationClass : operationClasses) {
      if (configurationType.isAssignableFrom(operationClass) || operationClass.isAssignableFrom(configurationType)) {
        throw new IllegalConfigurationModelDefinitionException(
                                                               format("Configuration class '%s' cannot be the same class (nor a derivative) of any operation class '%s",
                                                                      configurationType.getName(), operationClass.getName()));
      }
    }
  }
}
