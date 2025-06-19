/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;
import static org.mule.runtime.extension.api.dsl.syntax.DslSyntaxUtils.getSanitizedElementName;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getValidatedJavaVersionsIntersection;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.model.ExtensionModelHelper;
import org.mule.runtime.extension.api.loader.parser.ConfigurationModelParser;
import org.mule.runtime.extension.api.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.extension.api.loader.parser.ErrorModelParser;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParser;
import org.mule.runtime.extension.api.loader.parser.FunctionModelParser;
import org.mule.runtime.extension.api.loader.parser.OperationModelParser;
import org.mule.runtime.extension.api.loader.parser.SourceModelParser;
import org.mule.runtime.extension.api.runtime.exception.SdkExceptionHandlerFactory;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link ExtensionModelParser} implementation for Mule SDK extensions
 *
 * @since 4.5.0
 */
public abstract class MuleSdkExtensionModelParser extends BaseMuleSdkExtensionModelParser implements ExtensionModelParser {

  private final TypeLoader typeLoader;
  private List<OperationModelParser> operationModelParsers;
  private final ExtensionModelHelper extensionModelHelper;
  private Set<String> supportedJavaVersions;

  public MuleSdkExtensionModelParser(TypeLoader typeLoader, ExtensionModelHelper extensionModelHelper) {
    this.typeLoader = typeLoader;
    this.extensionModelHelper = extensionModelHelper;
  }

  protected void init(ArtifactAst ast) {
    supportedJavaVersions = getValidatedJavaVersionsIntersection(getName(), "Extension", ast.dependencies());
    operationModelParsers = computeOperationModelParsers(ast);
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return emptyList();
  }

  @Override
  public List<ConfigurationModelParser> getConfigurationParsers() {
    return emptyList();
  }

  @Override
  public List<OperationModelParser> getOperationModelParsers() {
    return operationModelParsers;
  }

  @Override
  public List<SourceModelParser> getSourceModelParsers() {
    return emptyList();
  }

  @Override
  public List<ConnectionProviderModelParser> getConnectionProviderModelParsers() {
    return emptyList();
  }

  @Override
  public List<FunctionModelParser> getFunctionModelParsers() {
    return emptyList();
  }

  @Override
  public List<ErrorModelParser> getErrorModelParsers() {
    return emptyList();
  }

  @Override
  public List<ExternalLibraryModel> getExternalLibraryModels() {
    return emptyList();
  }

  @Override
  public Optional<SdkExceptionHandlerFactory> getExceptionHandlerFactory() {
    return empty();
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return empty();
  }

  @Override
  public List<MetadataType> getExportedTypes() {
    return emptyList();
  }

  @Override
  public List<String> getExportedResources() {
    return emptyList();
  }

  @Override
  public List<MetadataType> getImportedTypes() {
    return emptyList();
  }

  @Override
  public List<String> getPrivilegedExportedArtifacts() {
    return emptyList();
  }

  @Override
  public List<String> getPrivilegedExportedPackages() {
    return emptyList();
  }

  @Override
  public Map<MetadataType, List<MetadataType>> getSubTypes() {
    return emptyMap();
  }

  @Override
  public List<NotificationModel> getNotificationModels() {
    return emptyList();
  }

  @Override
  public Optional<Class<? extends ArtifactLifecycleListener>> getArtifactLifecycleListenerClass() {
    return empty();
  }

  @Override
  public Set<String> getSupportedJavaVersions() {
    return supportedJavaVersions;
  }

  /**
   * @param ast the {@link ArtifactAst} representing the extension.
   * @return a {@link Stream} with the top level elements {@link ComponentAst} extracted from the {@code ast}.
   */
  protected abstract Stream<ComponentAst> getTopLevelElements(ArtifactAst ast);

  private List<OperationModelParser> computeOperationModelParsers(ArtifactAst ast) {
    final Map<String, MuleSdkOperationModelParser> operationParsersByName =
        getTopLevelElements(ast)
            .filter(c -> c.getComponentType() == OPERATION_DEF)
            .map(c -> createOperationModelParser(c, getNamespace()))
            .collect(toMap(c -> getSanitizedElementName(c::getName), identity()));

    // Some characteristics of the operation model parsers require knowledge about the other operation model parsers
    operationParsersByName.values()
        .forEach(operationModelParser -> operationModelParser.computeCharacteristics(operationParsersByName));

    return new ArrayList<>(operationParsersByName.values());
  }

  protected MuleSdkOperationModelParser createOperationModelParser(ComponentAst operation, String namespace) {
    return new MuleSdkOperationModelParser(operation, namespace, typeLoader, extensionModelHelper);
  }
}
