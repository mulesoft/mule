/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;
import static org.mule.runtime.extension.api.dsl.syntax.DslSyntaxUtils.getSanitizedElementName;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.internal.model.ExtensionModelHelper;
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConfigurationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.FunctionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.XmlDslConfiguration;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkExtensionModelMetadataParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * {@link ExtensionModelParser} implementation for Mule SDK extensions
 *
 * @since 4.5.0
 */
public class MuleSdkExtensionModelParser implements ExtensionModelParser {

  // The namespace of the extension when it's defined within the same artifact rather than in a separate artifact.
  public static final String ARTIFACT_LOCAL_EXTENSION_NAMESPACE = "THIS";

  private final MuleSdkExtensionModelMetadataParser metadataParser;
  private final Supplier<Stream<ComponentAst>> topLevelComponentsSupplier;
  private final TypeLoader typeLoader;
  private final List<OperationModelParser> operationModelParsers;
  private final ExtensionModelHelper extensionModelHelper;

  public MuleSdkExtensionModelParser(MuleSdkExtensionModelMetadataParser metadataParser,
                                     Supplier<Stream<ComponentAst>> topLevelComponentsSupplier,
                                     TypeLoader typeLoader,
                                     ExtensionModelHelper extensionModelHelper) {
    this.metadataParser = metadataParser;
    this.topLevelComponentsSupplier = topLevelComponentsSupplier;
    this.typeLoader = typeLoader;
    this.extensionModelHelper = extensionModelHelper;
    operationModelParsers = computeOperationModelParsers();
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return emptyList();
  }

  @Override
  public String getName() {
    return metadataParser.getName();
  }

  @Override
  public Category getCategory() {
    return metadataParser.getCategory();
  }

  @Override
  public String getVendor() {
    return metadataParser.getVendor();
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
  public LicenseModelProperty getLicenseModelProperty() {
    return metadataParser.getLicenseModelProperty();
  }

  @Override
  public List<ExternalLibraryModel> getExternalLibraryModels() {
    return emptyList();
  }

  @Override
  public Optional<ExceptionHandlerModelProperty> getExtensionHandlerModelProperty() {
    return empty();
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return empty();
  }

  @Override
  public Optional<XmlDslConfiguration> getXmlDslConfiguration() {
    return metadataParser.getXmlDslConfiguration();
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
  public String getNamespace() {
    return metadataParser.getNamespace();
  }

  @Override
  public Optional<SinceMuleVersionModelProperty> getSinceMuleVersionModelProperty() {
    return empty();
  }

  private List<OperationModelParser> computeOperationModelParsers() {
    final Map<String, MuleSdkOperationModelParserSdk> operationParsersByName =
        topLevelComponentsSupplier.get()
            .filter(c -> c.getComponentType() == OPERATION_DEF)
            .map(c -> new MuleSdkOperationModelParserSdk(c, typeLoader, extensionModelHelper))
            .collect(toMap(c -> getSanitizedElementName(c::getName), identity()));

    // Some characteristics of the operation model parsers require knowledge about the other operation model parsers
    operationParsersByName.values()
        .forEach(operationModelParser -> operationModelParser.computeCharacteristics(operationParsersByName));

    return new ArrayList<>(operationParsersByName.values());
  }
}
