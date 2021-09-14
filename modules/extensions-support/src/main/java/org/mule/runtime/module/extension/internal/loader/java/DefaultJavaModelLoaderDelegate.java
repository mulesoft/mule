/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory.getDefault;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils.getXmlDslModel;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getExtensionsNamespace;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegate;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.error.ErrorsModelFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParser;

import java.util.function.Supplier;

/**
 * Describes an {@link ExtensionModel} by analyzing the annotations in the class provided in the constructor
 *
 * @since 4.0
 */
public class DefaultJavaModelLoaderDelegate implements ModelLoaderDelegate {

  protected Class<?> extensionType;
  protected final ExtensionElement extensionElement;
  protected final String version;

  private final ConfigModelLoaderDelegate configLoaderDelegate = new ConfigModelLoaderDelegate(this);
  private final OperationModelLoaderDelegate operationLoaderDelegate = new OperationModelLoaderDelegate(this);
  private final FunctionModelLoaderDelegate functionModelLoaderDelegate = new FunctionModelLoaderDelegate(this);
  private final SourceModelLoaderDelegate sourceModelLoaderDelegate = new SourceModelLoaderDelegate(this);
  private final ConnectionProviderModelLoaderDelegate connectionProviderModelLoaderDelegate =
      new ConnectionProviderModelLoaderDelegate(this);
  private final ParameterModelsLoaderDelegate parameterModelsLoaderDelegate = new ParameterModelsLoaderDelegate();

  private Supplier<ErrorsModelFactory> errorsModelFactorySupplier;

  public DefaultJavaModelLoaderDelegate(ExtensionElement extensionElement, String version) {
    this.version = version;
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
    ExtensionModelParser parser = new JavaExtensionModelParser(extensionElement, context);
    ExtensionDeclarer declarer =
        context.getExtensionDeclarer()
            .named(parser.getName())
            .onVersion(version)
            .fromVendor(parser.getVendor())
            .withCategory(parser.getCategory())
            .withModelProperty(parser.getLicenseModelProperty())
            .withXmlDsl(getXmlDslModel(extensionElement, version, parser.getXmlDslConfiguration()));

    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    context.getParameter("COMPILATION_MODE")
        .ifPresent(m -> declarer.withModelProperty(new CompileTimeModelProperty()));

    parser.getDeprecationModel().ifPresent(declarer::withDeprecation);
    parser.getExternalLibraryModels().forEach(declarer::withExternalLibrary);
    parser.getExtensionHandlerModelProperty().ifPresent(declarer::withModelProperty);
    parser.getAdditionalModelProperties().forEach(declarer::withModelProperty);

    parseErrorModels(parser, declarer);

    configLoaderDelegate.declareConfigurations(declarer, parser);
    connectionProviderModelLoaderDelegate.declareConnectionProviders(declarer, parser.getConnectionProviderModelParsers());

    if (!isEmpty(extensionElement.getConfigurations())) {
      operationLoaderDelegate.declareOperations(declarer, declarer, parser, parser.getOperationModelParsers());
      functionModelLoaderDelegate.declareFunctions(declarer, parser.getFunctionModelParsers());
      sourceModelLoaderDelegate.declareMessageSources(declarer, declarer, parser.getSourceModelParsers());
    }

    return declarer;
  }

  private void parseErrorModels(ExtensionModelParser parser, ExtensionDeclarer declarer) {
    initErrorModelFactorySupplier(parser, declarer);
    new ErrorsModelFactory(getExtensionsNamespace(declarer.getDeclaration())).getErrorModels().forEach(declarer::withErrorModel);
    createErrorModelFactory().getErrorModels().forEach(declarer::withErrorModel);
  }

  private void initErrorModelFactorySupplier(ExtensionModelParser parser, ExtensionDeclarer declarer) {
    errorsModelFactorySupplier = () -> new ErrorsModelFactory(parser.getErrorModelParsers(), getExtensionsNamespace(declarer.getDeclaration()));
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

  ErrorsModelFactory createErrorModelFactory() {
    checkState(errorsModelFactorySupplier != null, "errorModelFactorySupplier not yet initialized");
    return errorsModelFactorySupplier.get();
  }

  public ParameterModelsLoaderDelegate getParameterModelsLoaderDelegate() {
    return parameterModelsLoaderDelegate;
  }
}
