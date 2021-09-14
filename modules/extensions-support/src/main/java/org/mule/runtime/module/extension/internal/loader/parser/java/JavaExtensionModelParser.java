/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.getExceptionEnricherFactory;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getInfoFromExtension;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getRequiresEnterpriseLicenseInfo;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getRequiresEntitlementInfo;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.parseExternalLibraryModels;

import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
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
import org.mule.runtime.module.extension.internal.loader.parser.java.error.JavaErrorModelParserUtils;
import org.mule.runtime.module.extension.internal.loader.parser.java.info.RequiresEnterpriseLicenseInfo;
import org.mule.runtime.module.extension.internal.loader.parser.java.info.RequiresEntitlementInfo;

import java.util.List;
import java.util.Optional;

/**
 * {@link ExtensionModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaExtensionModelParser extends AbstractModelParser implements ExtensionModelParser {

  private String namespace;
  private Optional<XmlDslConfiguration> xmlDslConfiguration;
  private List<ErrorModelParser> errorModelParsers;

  public JavaExtensionModelParser(ExtensionElement extensionElement, ExtensionLoadingContext loadingContext) {
    super(extensionElement, loadingContext);
    parseStructure(extensionElement);
  }

  private void parseStructure(ExtensionElement extensionElement) {
    xmlDslConfiguration = parseXmlDslConfiguration();
    namespace = xmlDslConfiguration
        .map(dsl -> dsl.getPrefix().toLowerCase())
        .orElseThrow(() -> new IllegalModelDefinitionException(format("Extension '%s' does not properly declare a namespace",
                                                                      getName())));

    errorModelParsers = fetchErrorModelParsers();

    additionalModelProperties.add(new ExtensionTypeDescriptorModelProperty(extensionElement));
    extensionElement.getDeclaringClass()
        .ifPresent(extensionClass -> additionalModelProperties.add(new ImplementingTypeModelProperty(extensionClass)));
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
    return JavaExtensionModelParserUtils.getOperationParsers(this,
                                                             extensionElement,
                                                             extensionElement,
                                                             loadingContext);
  }

  @Override
  public List<SourceModelParser> getSourceModelParsers() {
    return JavaExtensionModelParserUtils.getSourceParsers(extensionElement,
                                                          extensionElement.getSources(),
                                                          loadingContext);
  }

  @Override
  public List<ConnectionProviderModelParser> getConnectionProviderModelParsers() {
    return JavaExtensionModelParserUtils.getConnectionProviderModelParsers(extensionElement,
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
    return JavaErrorModelParserUtils.parseExtensionErrorModels(extensionElement, this);
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
    return getExceptionEnricherFactory(extensionElement).map(ExceptionHandlerModelProperty::new);
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
  public String getExtensionNamespace() {
    return namespace;
  }

  private Optional<XmlDslConfiguration> parseXmlDslConfiguration() {
    return getInfoFromExtension(
                                extensionElement,
                                Xml.class,
                                org.mule.sdk.api.annotation.dsl.xml.Xml.class,
                                xml -> new XmlDslConfiguration(xml.prefix(), xml.namespace()),
                                xml -> new XmlDslConfiguration(xml.prefix(), xml.namespace()));
  }
}
