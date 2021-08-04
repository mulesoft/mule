/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DEFAULT_CONNECTION_PROVIDER_NAME;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.DefaultConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class JavaConnectionProviderModelParser implements ConnectionProviderModelParser {

  private final ConnectionProviderElement element;
  private final ClassTypeLoader typeLoader;
  private final ExtensionLoadingContext extensionLoadingContext;

  private final List<ModelProperty> additionalModelProperties = new LinkedList<>();
  private final ClassLoader extensionClassLoader;

  public JavaConnectionProviderModelParser(ExtensionElement extensionElement,
                                           ConnectionProviderElement element,
                                           ClassTypeLoader typeLoader,
                                           ExtensionLoadingContext extensionLoadingContext) {
    this.element = element;
    this.typeLoader = typeLoader;
    this.extensionLoadingContext = extensionLoadingContext;
    extensionClassLoader = extensionElement.getDeclaringClass()
        .map(Class::getClassLoader)
        .orElse(ExtensionModel.class.getClassLoader());

    parseStructure();
  }

  private void parseStructure() {



  }

  private void collectAdditionalModelProperties() {
    List<Type> providerGenerics = element.getSuperTypeGenerics(ConnectionProvider.class);

    if (providerGenerics.size() != 1) {
      // TODO: MULE-9220: Add a syntax validator for this
      throw new IllegalConnectionProviderModelDefinitionException(
          format("Connection provider class '%s' was expected to have 1 generic type "
                  + "(for the connection type) but %d were found",
              element.getName(), providerGenerics.size()));
    }

    additionalModelProperties.add(new ConnectionTypeModelProperty(providerGenerics.get(0)));
    element.getDeclaringClass().ifPresent(clazz -> additionalModelProperties.add(new ImplementingTypeModelProperty(clazz)));
    additionalModelProperties.add(new ExtensionTypeDescriptorModelProperty(element));
  }

  @Override
  public String getName() {
    if (element.getName().equals(element.getAlias())) {
      return DEFAULT_CONNECTION_PROVIDER_NAME;
    }

    return element.getAlias();
  }

  @Override
  public String getDescription() {
    return element.getDescription();
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
    return null;
  }

  @Override
  public List<ExternalLibraryModel> getExternalLibraryModels() {
    return null;
  }

  @Override
  public ConnectionManagementType getConnectionManagementType() {
    return null;
  }

  @Override
  public ConnectionProviderFactoryModelProperty getConnectionProviderFactoryModelProperty() {
    return new ConnectionProviderFactoryModelProperty(new DefaultConnectionProviderFactory(
        element.getDeclaringClass().get(),
        extensionClassLoader));
  }

  @Override
  public boolean supportsConnectivityTesting() {
    return false;
  }

  @Override
  public boolean isExcludedFromConnectivitySchema() {
    return false;
  }

  @Override
  public Optional<OAuthModelProperty> getOAuthModelProperty() {
    return Optional.empty();
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return null;
  }
}
