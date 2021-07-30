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
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionDefinitionParserUtils.parseExternalLibraryModels;

import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.extension.api.annotation.license.RequiresEnterpriseLicense;
import org.mule.runtime.extension.api.annotation.license.RequiresEntitlement;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionConfigurationDefinitionParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionDefinitionParser;

import java.util.List;
import java.util.Optional;

public class JavaExtensionDefinitionParser implements ExtensionDefinitionParser {

  private final ExtensionElement extensionElement;

  public JavaExtensionDefinitionParser(ExtensionElement extensionElement) {
    this.extensionElement = extensionElement;
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
  public List<ExtensionConfigurationDefinitionParser> getConfigurationParsers() {
    List<ConfigurationElement> configurations = extensionElement.getConfigurations();
    if (configurations.isEmpty()) {
      return singletonList(new JavaExtensionConfigurationDefinitionParser(extensionElement, extensionElement));
    } else {
      return configurations.stream()
          .map(config -> new JavaExtensionConfigurationDefinitionParser(extensionElement, config))
          .collect(toList());
    }
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


}
