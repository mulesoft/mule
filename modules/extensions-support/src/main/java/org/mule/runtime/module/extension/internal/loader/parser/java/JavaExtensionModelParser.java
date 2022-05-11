/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.extension.internal.util.ExtensionNamespaceUtils.getExtensionsNamespace;
import static org.mule.runtime.module.extension.internal.loader.ModelLoaderDelegateUtils.requiresConfig;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceRepeatableAnnotation;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getOperationParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getRequiresEnterpriseLicenseInfo;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getRequiresEntitlementInfo;
import static org.mule.runtime.module.extension.internal.loader.parser.java.error.JavaErrorModelParserUtils.getExceptionHandlerModelProperty;
import static org.mule.runtime.module.extension.internal.loader.parser.java.error.JavaErrorModelParserUtils.parseExtensionErrorModels;
import static org.mule.runtime.module.extension.internal.loader.parser.java.lib.JavaExternalLibModelParserUtils.parseExternalLibraryModels;
import static org.mule.runtime.module.extension.internal.loader.parser.java.notification.NotificationModelParserUtils.parseLegacyNotifications;
import static org.mule.runtime.module.extension.internal.loader.parser.java.notification.NotificationModelParserUtils.parseNotifications;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.getXmlDslModel;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.ImportedTypes;
import org.mule.runtime.extension.api.annotation.PrivilegedExport;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.SubTypesMapping;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.notification.NotificationActions;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.delegate.StereotypeModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExportedClassNamesModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConfigurationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.FunctionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.XmlDslConfiguration;
import org.mule.runtime.module.extension.internal.loader.parser.java.info.ExportInfo;
import org.mule.runtime.module.extension.internal.loader.parser.java.info.RequiresEnterpriseLicenseInfo;
import org.mule.runtime.module.extension.internal.loader.parser.java.info.RequiresEntitlementInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ExtensionModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaExtensionModelParser extends AbstractJavaModelParser implements ExtensionModelParser {

  private Optional<XmlDslConfiguration> xmlDslConfiguration;
  private List<ErrorModelParser> errorModelParsers;
  private final List<MetadataType> exportedTypes = new LinkedList<>();
  private final List<String> exportedResources = new LinkedList<>();
  private final ClassTypeLoader typeLoader;
  private final StereotypeModelLoaderDelegate stereotypeLoaderDelegate;

  private List<MetadataType> importedTypes = new LinkedList<>();
  private List<String> privilegedExportedArtifacts = new LinkedList<>();
  private List<String> privilegedExportedPackages = new LinkedList<>();
  private List<NotificationModel> notificationModels = new LinkedList<>();
  private Map<MetadataType, List<MetadataType>> subTypes = new LinkedHashMap<>();
  private String namespace;

  public JavaExtensionModelParser(ExtensionElement extensionElement, ExtensionLoadingContext loadingContext) {
    this(extensionElement, new StereotypeModelLoaderDelegate(loadingContext), loadingContext);
  }

  public JavaExtensionModelParser(ExtensionElement extensionElement,
                                  StereotypeModelLoaderDelegate stereotypeLoaderDelegate,
                                  ExtensionLoadingContext loadingContext) {
    super(extensionElement, loadingContext);
    this.stereotypeLoaderDelegate = stereotypeLoaderDelegate;
    typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(loadingContext.getExtensionClassLoader());
    parseStructure(extensionElement);
  }

  private void parseStructure(ExtensionElement extensionElement) {
    xmlDslConfiguration = parseXmlDslConfiguration();

    // use dummy version since this is just for obtaining the namespace
    namespace = getExtensionsNamespace(getXmlDslModel(extensionElement, "1.0.0", xmlDslConfiguration));
    stereotypeLoaderDelegate.setNamespace(namespace);
    errorModelParsers = fetchErrorModelParsers();

    additionalModelProperties.add(new ExtensionTypeDescriptorModelProperty(extensionElement));
    extensionElement.getDeclaringClass()
        .ifPresent(extensionClass -> additionalModelProperties.add(new ImplementingTypeModelProperty(extensionClass)));

    parseExported();
    parseImportedTypes();
    parseSubtypes();
    parseNotificationModels();
  }

  private void parseSubtypes() {
    List<Pair<Type, List<Type>>> pairs = mapReduceRepeatableAnnotation(
                                                                       extensionElement,
                                                                       SubTypeMapping.class,
                                                                       org.mule.sdk.api.annotation.SubTypeMapping.class,
                                                                       container -> ((SubTypesMapping) container).value(),
                                                                       container -> ((org.mule.sdk.api.annotation.SubTypesMapping) container)
                                                                           .value(),
                                                                       value -> new Pair<>(value
                                                                           .getClassValue(sub -> sub.baseType()),
                                                                                           value.getClassArrayValue(sub -> sub
                                                                                               .subTypes())),
                                                                       value -> new Pair<>(value
                                                                           .getClassValue(sub -> sub.baseType()),
                                                                                           value.getClassArrayValue(sub -> sub
                                                                                               .subTypes()))).collect(toList());

    Set<Object> baseTypes = new HashSet<>();
    pairs.forEach(mapping -> {

      final Type baseType = mapping.getFirst();
      baseTypes.add(baseType);

      MetadataType baseMetadataType = baseType.asMetadataType();
      Map<Type, MetadataType> subTypesMetadataTypes = mapping.getSecond()
          .stream()
          .collect(toMap(identity(), Type::asMetadataType, (u, v) -> {
            throw new IllegalStateException(format("Duplicate key %s", u));
          }, LinkedHashMap::new));

      subTypes.put(baseMetadataType, new ArrayList<>(subTypesMetadataTypes.values()));
    });
  }

  private void parseNotificationModels() {
    notificationModels = mapReduceSingleAnnotation(extensionElement,
                                                   NotificationActions.class,
                                                   org.mule.sdk.api.annotation.notification.NotificationActions.class,
                                                   value -> parseLegacyNotifications(value, namespace, typeLoader),
                                                   value -> parseNotifications(value, namespace, typeLoader))
                                                       .orElse(new LinkedList<>());
  }

  private void parseImportedTypes() {
    List<Type> types = mapReduceRepeatableAnnotation(extensionElement,
                                                     Import.class,
                                                     org.mule.sdk.api.annotation.Import.class,
                                                     container -> ((ImportedTypes) container).value(),
                                                     container -> ((org.mule.sdk.api.annotation.ImportedTypes) container).value(),
                                                     value -> value.getClassValue(Import::type),
                                                     value -> value.getClassValue(org.mule.sdk.api.annotation.Import::type))
                                                         .collect(toList());

    importedTypes = types.stream()
        .distinct()
        .map(Type::asMetadataType)
        .collect(toList());

    if (types.size() != importedTypes.size()) {
      throw new IllegalModelDefinitionException(
                                                format("There should be only one Import declaration for any given type in extension [%s]."
                                                    + " Multiple imports of the same type are not allowed", getName()));
    }
  }

  private void parseExported() {
    ExportInfo info = mapReduceSingleAnnotation(extensionElement,
                                                Export.class,
                                                org.mule.sdk.api.annotation.Export.class,
                                                export -> ExportInfo.fromLegacy(export),
                                                export -> ExportInfo.fromSdkApi(export)).orElse(null);

    if (info == null) {
      return;
    }

    Set<String> exportedClassNames = new LinkedHashSet<>();
    info.getExportedTypes().forEach(type -> {
      exportedClassNames.add(type.getClassInformation().getClassname());
      exportedTypes.add(type.asMetadataType());
    });
    exportedResources.addAll(info.getExportedResources());

    if (!exportedClassNames.isEmpty()) {
      additionalModelProperties.add(new ExportedClassNamesModelProperty(exportedClassNames));
    }

    parsePrivilegeExport();
  }

  private void parsePrivilegeExport() {
    mapReduceSingleAnnotation(
                              extensionElement,
                              PrivilegedExport.class,
                              org.mule.sdk.api.annotation.PrivilegedExport.class,
                              value -> new Pair<>(value.getArrayValue(PrivilegedExport::artifacts),
                                                  value.getArrayValue(PrivilegedExport::packages)),
                              value -> new Pair<>(value.getArrayValue(org.mule.sdk.api.annotation.PrivilegedExport::artifacts),
                                                  value.getArrayValue(org.mule.sdk.api.annotation.PrivilegedExport::packages)))
                                                      .ifPresent(exported -> {
                                                        privilegedExportedArtifacts = exported.getFirst();
                                                        privilegedExportedPackages = exported.getSecond();
                                                      });
  }

  @Override
  public String getName() {
    return extensionElement.getName();
  }

  @Override
  public Category getCategory() {
    return extensionElement.getCategory();
  }

  @Override
  public String getVendor() {
    return extensionElement.getVendor();
  }

  @Override
  public List<ConfigurationModelParser> getConfigurationParsers() {
    List<ConfigurationElement> configurations = extensionElement.getConfigurations();
    if (configurations.isEmpty()) {
      return singletonList(new JavaConfigurationModelParser(this, extensionElement, extensionElement, loadingContext));
    } else {
      return configurations.stream()
          .map(config -> new JavaConfigurationModelParser(this, extensionElement, config, loadingContext))
          .collect(toList());
    }
  }

  @Override
  public List<OperationModelParser> getOperationModelParsers() {
    return getOperationParsers(this,
                               extensionElement,
                               extensionElement,
                               loadingContext)
                                   .filter(operation -> !requiresConfig(operation))
                                   .collect(toList());
  }

  @Override
  public List<SourceModelParser> getSourceModelParsers() {
    return JavaExtensionModelParserUtils.getSourceParsers(extensionElement,
                                                          extensionElement.getSources(),
                                                          loadingContext)
        .filter(source -> !requiresConfig(source))
        .collect(toList());
  }

  @Override
  public List<ConnectionProviderModelParser> getConnectionProviderModelParsers() {
    return JavaExtensionModelParserUtils.getConnectionProviderModelParsers(this, extensionElement,
                                                                           extensionElement.getConnectionProviders());
  }

  @Override
  public List<FunctionModelParser> getFunctionModelParsers() {
    return JavaExtensionModelParserUtils.getFunctionModelParsers(extensionElement,
                                                                 extensionElement.getFunctionContainers(),
                                                                 loadingContext);
  }

  @Override
  public List<ErrorModelParser> getErrorModelParsers() {
    return errorModelParsers;
  }

  private List<ErrorModelParser> fetchErrorModelParsers() {
    return parseExtensionErrorModels(extensionElement);
  }

  @Override
  public LicenseModelProperty getLicenseModelProperty() {
    Optional<RequiresEntitlementInfo> requiresEntitlementOptional = getRequiresEntitlementInfo(extensionElement);
    Optional<RequiresEnterpriseLicenseInfo> requiresEnterpriseLicenseOptional =
        getRequiresEnterpriseLicenseInfo(extensionElement);
    boolean requiresEnterpriseLicense = requiresEnterpriseLicenseOptional.isPresent();
    boolean allowsEvaluationLicense =
        requiresEnterpriseLicenseOptional.map(RequiresEnterpriseLicenseInfo::isAllowEvaluationLicense).orElse(true);
    Optional<String> requiredEntitlement = requiresEntitlementOptional.map(RequiresEntitlementInfo::getName);

    return new LicenseModelProperty(requiresEnterpriseLicense, allowsEvaluationLicense, requiredEntitlement);
  }

  @Override
  public List<ExternalLibraryModel> getExternalLibraryModels() {
    return parseExternalLibraryModels(extensionElement);
  }

  @Override
  public Optional<ExceptionHandlerModelProperty> getExtensionHandlerModelProperty() {
    return getExceptionHandlerModelProperty(extensionElement, "Extension", getName());
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return JavaExtensionModelParserUtils.getDeprecationModel(extensionElement);
  }

  @Override
  public Optional<XmlDslConfiguration> getXmlDslConfiguration() {
    return xmlDslConfiguration;
  }

  @Override
  public List<MetadataType> getExportedTypes() {
    return exportedTypes;
  }

  @Override
  public Map<MetadataType, List<MetadataType>> getSubTypes() {
    return subTypes;
  }

  @Override
  public List<String> getExportedResources() {
    return exportedResources;
  }

  @Override
  public List<String> getPrivilegedExportedArtifacts() {
    return privilegedExportedArtifacts;
  }

  @Override
  public List<String> getPrivilegedExportedPackages() {
    return privilegedExportedPackages;
  }

  @Override
  public List<MetadataType> getImportedTypes() {
    return importedTypes;
  }

  @Override
  public List<NotificationModel> getNotificationModels() {
    return notificationModels;
  }

  public StereotypeModelLoaderDelegate getStereotypeLoaderDelegate() {
    return stereotypeLoaderDelegate;
  }

  private Optional<XmlDslConfiguration> parseXmlDslConfiguration() {
    return mapReduceSingleAnnotation(
                                     extensionElement,
                                     Xml.class,
                                     org.mule.sdk.api.annotation.dsl.xml.Xml.class,
                                     xml -> new XmlDslConfiguration(
                                                                    xml.getStringValue(Xml::prefix),
                                                                    xml.getStringValue(Xml::namespace)),
                                     xml -> new XmlDslConfiguration(
                                                                    xml.getStringValue(org.mule.sdk.api.annotation.dsl.xml.Xml::prefix),
                                                                    xml.getStringValue(org.mule.sdk.api.annotation.dsl.xml.Xml::namespace)));
  }
}
