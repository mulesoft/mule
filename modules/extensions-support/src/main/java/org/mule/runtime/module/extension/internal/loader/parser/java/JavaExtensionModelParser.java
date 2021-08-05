/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.getExceptionEnricherFactory;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.parseExternalLibraryModels;

import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.annotation.license.RequiresEnterpriseLicense;
import org.mule.runtime.extension.api.annotation.license.RequiresEntitlement;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConfigurationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.FunctionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class JavaExtensionModelParser implements ExtensionModelParser {

  private final ExtensionElement extensionElement;
  private final ExtensionLoadingContext loadingContext;
  private final List<ModelProperty> additionalModelProperties = new LinkedList<>();

  public JavaExtensionModelParser(ExtensionElement extensionElement, ExtensionLoadingContext loadingContext) {
    this.extensionElement = extensionElement;
    this.loadingContext = loadingContext;
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
      return singletonList(new JavaConfigurationModelParser(extensionElement, extensionElement, loadingContext));
    } else {
      return configurations.stream()
          .map(config -> new JavaConfigurationModelParser(extensionElement, config, loadingContext))
          .collect(toList());
    }
  }

  @Override
  public List<OperationModelParser> getOperationModelParsers() {
    return JavaExtensionModelParserUtils.getOperationParsers(
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
  public LicenseModelProperty getLicenseModelProperty() {
    Optional<RequiresEntitlement> requiresEntitlementOptional = extensionElement.getAnnotation(RequiresEntitlement.class);
    Optional<RequiresEnterpriseLicense> requiresEnterpriseLicenseOptional =
        extensionElement.getAnnotation(RequiresEnterpriseLicense.class);
    boolean requiresEnterpriseLicense = requiresEnterpriseLicenseOptional.isPresent();
    boolean allowsEvaluationLicense =
        requiresEnterpriseLicenseOptional.map(RequiresEnterpriseLicense::allowEvaluationLicense).orElse(true);
    Optional<String> requiredEntitlement = requiresEntitlementOptional.map(RequiresEntitlement::name);

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
  public List<ModelProperty> getAdditionalModelProperties() {
    additionalModelProperties.add(new ExtensionTypeDescriptorModelProperty(extensionElement));
    extensionElement.getDeclaringClass()
        .ifPresent(extensionClass -> additionalModelProperties.add(new ImplementingTypeModelProperty(extensionClass)));

    return additionalModelProperties;
  }
}
