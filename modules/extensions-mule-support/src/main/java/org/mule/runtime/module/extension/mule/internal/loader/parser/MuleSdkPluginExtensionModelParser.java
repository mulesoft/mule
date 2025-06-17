/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.module.extension.internal.loader.ExtensionDevelopmentFramework.MULE_SDK;
import static org.mule.runtime.module.extension.internal.loader.utils.ExtensionNamespaceUtils.getExtensionsNamespace;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.getXmlDslModel;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_ALLOWS_EVALUATION_LICENSE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_CATEGORY_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DESCRIPTION_IDENTIFIER;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_LICENSING_COMPONENT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_NAMESPACE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_NAME_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_PREFIX_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_REQUIRED_ENTITLEMENT_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_REQUIRES_ENTERPRISE_LICENSE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_VENDOR_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_XML_DSL_ATTRIBUTES_COMPONENT_NAME;

import static java.lang.String.format;
import static java.util.Locale.getDefault;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.version.MuleMinorVersion;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.model.ExtensionModelHelper;
import org.mule.runtime.module.extension.internal.loader.ExtensionDevelopmentFramework;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.XmlDslConfiguration;
import org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link ExtensionModelParser} implementation for Mule SDK plugins
 *
 * @since 4.5.0
 */
public class MuleSdkPluginExtensionModelParser extends MuleSdkExtensionModelParser {

  private static final String MIN_MULE_VERSION = "4.5";
  private String name;
  private Category category;
  private String vendor;
  private String namespace;
  private Optional<XmlDslConfiguration> xmlDslConfiguration;
  private LicenseModelProperty licenseModelProperty;
  private List<ErrorModelParser> errorModelParsers;

  public MuleSdkPluginExtensionModelParser(ArtifactAst ast, TypeLoader typeLoader, ExtensionModelHelper extensionModelHelper) {
    super(typeLoader, extensionModelHelper);
    init(ast);
  }

  @Override
  protected void init(ArtifactAst ast) {
    parseStructure(ast);
    super.init(ast);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Category getCategory() {
    return category;
  }

  @Override
  public String getVendor() {
    return vendor;
  }

  @Override
  public Optional<XmlDslConfiguration> getXmlDslConfiguration() {
    return xmlDslConfiguration;
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public Optional<ResolvedMinMuleVersion> getResolvedMinMuleVersion() {
    return of(new ResolvedMinMuleVersion(name, new MuleMinorVersion(MIN_MULE_VERSION),
                                         format("Plugin %s has min mule version %s because the Mule Sdk was introduced in that version.",
                                                name, MIN_MULE_VERSION)));
  }

  @Override
  public LicenseModelProperty getLicenseModelProperty() {
    return licenseModelProperty;
  }

  @Override
  public ExtensionDevelopmentFramework getDevelopmentFramework() {
    return MULE_SDK;
  }

  @Override
  protected Stream<ComponentAst> getTopLevelElements(ArtifactAst ast) {
    return ast.topLevelComponentsStream();
  }

  private void parseStructure(ArtifactAst ast) {
    ComponentAst descriptionComponentAst =
        getRequiredTopLevelComponent(ast, MULE_SDK_EXTENSION_DESCRIPTION_IDENTIFIER);
    name = getParameter(descriptionComponentAst, MULE_SDK_EXTENSION_NAME_PARAMETER_NAME);
    category = Category
        .valueOf(this.<String>getParameter(descriptionComponentAst, MULE_SDK_EXTENSION_CATEGORY_PARAMETER_NAME).toUpperCase());
    vendor = getParameter(descriptionComponentAst, MULE_SDK_EXTENSION_VENDOR_PARAMETER_NAME);

    parseXmlDslConfiguration(descriptionComponentAst);
    parseLicenseModelProperty(descriptionComponentAst);

    // use dummy version since this is just for obtaining the namespace
    this.namespace = getExtensionsNamespace(getXmlDslModel(name, "1.0.0", xmlDslConfiguration));

    parseErrorsDeclaration(ast);
  }

  private void parseErrorsDeclaration(ArtifactAst ast) {
    errorModelParsers =
        new ArrayList<>(new MuleSdkErrorsDeclarationParser(ast, namespace.toUpperCase(getDefault())).parse()
            .values());
  }

  @Override
  public List<ErrorModelParser> getErrorModelParsers() {
    return errorModelParsers;
  }

  private void parseXmlDslConfiguration(ComponentAst descriptionComponentAst) {
    xmlDslConfiguration = getSingleChild(descriptionComponentAst, MULE_SDK_EXTENSION_XML_DSL_ATTRIBUTES_COMPONENT_NAME)
        .map(xmlDslAttributesComponentAst -> {
          Optional<String> prefix = getOptionalParameter(xmlDslAttributesComponentAst, MULE_SDK_EXTENSION_PREFIX_PARAMETER_NAME);
          Optional<String> namespace =
              getOptionalParameter(xmlDslAttributesComponentAst, MULE_SDK_EXTENSION_NAMESPACE_PARAMETER_NAME);
          if (prefix.isPresent() || namespace.isPresent()) {
            return of(new XmlDslConfiguration(prefix.orElse(""), namespace.orElse("")));
          } else {
            return Optional.<XmlDslConfiguration>empty();
          }
        })
        .orElse(empty());
  }

  private void parseLicenseModelProperty(ComponentAst descriptionComponentAst) {
    licenseModelProperty = getSingleChild(descriptionComponentAst, MULE_SDK_EXTENSION_LICENSING_COMPONENT_NAME)
        .map(licensingComponentAst -> {
          boolean requiresEeLicense =
              getParameter(licensingComponentAst, MULE_SDK_EXTENSION_REQUIRES_ENTERPRISE_LICENSE_PARAMETER_NAME);
          boolean allowsEvaluationLicense = getParameter(licensingComponentAst,
                                                         MULE_SDK_EXTENSION_ALLOWS_EVALUATION_LICENSE_PARAMETER_NAME);
          Optional<String> requiredEntitlement = getOptionalParameter(licensingComponentAst,
                                                                      MULE_SDK_EXTENSION_REQUIRED_ENTITLEMENT_PARAMETER_NAME);
          return new LicenseModelProperty(requiresEeLicense, allowsEvaluationLicense, requiredEntitlement);
        })
        .orElse(new LicenseModelProperty(false, true, empty()));
  }
}
