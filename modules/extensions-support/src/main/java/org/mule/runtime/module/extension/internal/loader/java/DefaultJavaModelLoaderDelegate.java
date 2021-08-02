/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory.getDefault;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegate;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.WithParameters;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParser;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Describes an {@link ExtensionModel} by analyzing the annotations in the class provided in the constructor
 *
 * @since 4.0
 */
public class DefaultJavaModelLoaderDelegate implements ModelLoaderDelegate {

  protected Class<?> extensionType;
  protected final ExtensionElement extensionElement;
  protected final ClassTypeLoader typeLoader;
  protected final ExtensionModelParser extensionModelParser;
  protected final String version;

  private final ConfigModelLoaderDelegate configLoaderDelegate = new ConfigModelLoaderDelegate(this);
  private final OperationModelLoaderDelegate operationLoaderDelegate = new OperationModelLoaderDelegate(this);
  private final FunctionModelLoaderDelegate functionModelLoaderDelegate = new FunctionModelLoaderDelegate(this);
  private final SourceModelLoaderDelegate sourceModelLoaderDelegate = new SourceModelLoaderDelegate(this);
  private final ConnectionProviderModelLoaderDelegate connectionProviderModelLoaderDelegate =
      new ConnectionProviderModelLoaderDelegate(this);

  public DefaultJavaModelLoaderDelegate(ExtensionElement extensionElement, String version) {
    this.version = version;
    this.typeLoader = getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());
    this.extensionElement = extensionElement;
    extensionModelParser = new JavaExtensionModelParser(extensionElement);
  }


  public DefaultJavaModelLoaderDelegate(Class<?> extensionType, String version) {
    this(new ExtensionTypeWrapper<>(extensionType, getDefault().createTypeLoader(extensionType.getClassLoader())), version);
    this.extensionType = extensionType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionDeclarer declare(ExtensionLoadingContext context) {
    ExtensionDeclarer declarer =
        context.getExtensionDeclarer()
            .named(extensionModelParser.getName())
            .onVersion(version)
            .fromVendor(extensionModelParser.getVendor())
            .withCategory(extensionModelParser.getCategory())
            .withModelProperty(extensionModelParser.getLicenseModelProperty());

    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    context.getParameter("COMPILATION_MODE")
        .ifPresent(m -> declarer.withModelProperty(new CompileTimeModelProperty()));

    extensionModelParser.getExternalLibraryModels().forEach(declarer::withExternalLibrary);
    extensionModelParser.getExtensionHandlerModelProperty().ifPresent(declarer::withModelProperty);
    extensionModelParser.getAdditionalModelProperties().forEach(declarer::withModelProperty);

    configLoaderDelegate.declareConfigurations(declarer, extensionModelParser, context);
    connectionProviderModelLoaderDelegate.declareConnectionProviders(declarer, extensionElement);

    if (!isEmpty(extensionElement.getConfigurations())) {
      operationLoaderDelegate
          .declareOperations(declarer, declarer, null, extensionElement.getOperations(), false, context);

      functionModelLoaderDelegate
          .declareFunctions(declarer, declarer, null, extensionElement.getFunctions(), context);

      extensionElement.getSources()
          .forEach(source -> sourceModelLoaderDelegate.declareMessageSource(declarer, declarer, source, false, context));
    }

    return declarer;
  }

  boolean isInvalidConfigSupport(boolean supportsConfig, Optional<ExtensionParameter>... parameters) {
    return !supportsConfig && Stream.of(parameters).anyMatch(Optional::isPresent);
  }

  Declarer selectDeclarerBasedOnConfig(ExtensionDeclarer extensionDeclarer,
                                       Declarer declarer,
                                       Optional<ExtensionParameter>... parameters) {

    for (Optional<ExtensionParameter> parameter : parameters) {
      if (parameter.isPresent()) {
        return declarer;
      }
    }

    return extensionDeclarer;
  }

  Optional<ExtensionParameter> getConfigParameter(WithParameters element) {
    return element.getParametersAnnotatedWith(Config.class).stream().findFirst();
  }

  Optional<ExtensionParameter> getConnectionParameter(WithParameters element) {
    return element.getParametersAnnotatedWith(Connection.class).stream().findFirst();
  }

  ConfigModelLoaderDelegate getConfigLoaderDelegate() {
    return configLoaderDelegate;
  }

  OperationModelLoaderDelegate getOperationLoaderDelegate() {
    return operationLoaderDelegate;
  }

  FunctionModelLoaderDelegate getFunctionModelLoaderDelegate() {
    return functionModelLoaderDelegate;
  }

  SourceModelLoaderDelegate getSourceModelLoaderDelegate() {
    return sourceModelLoaderDelegate;
  }

  ConnectionProviderModelLoaderDelegate getConnectionProviderModelLoaderDelegate() {
    return connectionProviderModelLoaderDelegate;
  }

  Class<?> getExtensionType() {
    return extensionElement.getDeclaringClass().orElse(null);
  }

  ExtensionElement getExtensionElement() {
    return extensionElement;
  }

  protected ParameterModelsLoaderDelegate getFieldParametersLoader() {
    return fieldParametersLoader;
  }

  protected ParameterModelsLoaderDelegate getMethodParametersLoader() {
    return methodParametersLoader;
  }
}
