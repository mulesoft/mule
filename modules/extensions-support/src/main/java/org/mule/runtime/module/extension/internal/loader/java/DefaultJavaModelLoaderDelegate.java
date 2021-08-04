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
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegate;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
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
  protected final String version;

  private final ConfigModelLoaderDelegate configLoaderDelegate = new ConfigModelLoaderDelegate(this);
  private final OperationModelLoaderDelegate operationLoaderDelegate = new OperationModelLoaderDelegate(this);
  private final FunctionModelLoaderDelegate functionModelLoaderDelegate = new FunctionModelLoaderDelegate(this);
  private final SourceModelLoaderDelegate sourceModelLoaderDelegate = new SourceModelLoaderDelegate(this);
  private final ConnectionProviderModelLoaderDelegate connectionProviderModelLoaderDelegate =
      new ConnectionProviderModelLoaderDelegate(this);
  private final ParameterModelsLoaderDelegate parameterModelsLoaderDelegate = new ParameterModelsLoaderDelegate();

  public DefaultJavaModelLoaderDelegate(ExtensionElement extensionElement, String version) {
    this.version = version;
    this.typeLoader = getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());
    this.extensionElement = extensionElement;
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
    ExtensionModelParser extensionModelParser = new JavaExtensionModelParser(extensionElement, context);
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

    configLoaderDelegate.declareConfigurations(declarer, extensionModelParser);
    connectionProviderModelLoaderDelegate.declareConnectionProviders(declarer, extensionElement);

    if (!isEmpty(extensionElement.getConfigurations())) {
      operationLoaderDelegate
          .declareOperations(declarer, declarer, null);

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

  public ParameterModelsLoaderDelegate getParameterModelsLoaderDelegate() {
    return parameterModelsLoaderDelegate;
  }

  Class<?> getExtensionType() {
    return extensionElement.getDeclaringClass().orElse(null);
  }

  ExtensionElement getExtensionElement() {
    return extensionElement;
  }
}
