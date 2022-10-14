/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.extension.internal.util.ExtensionNamespaceUtils.getExtensionsNamespace;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.getXmlDslModel;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_ALLOWS_EVALUATION_LICENSE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_CATEGORY_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_NAMESPACE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_NAME_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_PREFIX_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_REQUIRED_ENTITLEMENT_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_REQUIRES_ENTERPRISE_LICENSE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_VENDOR_PARAMETER_NAME;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.internal.model.ExtensionModelHelper;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.XmlDslConfiguration;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link ExtensionModelParser} implementation for Mule SDK plugins
 *
 * @since 4.5.0
 */
public class MuleSdkPluginExtensionModelParser extends MuleSdkExtensionModelParser {

  private String name;
  private Category category;
  private String vendor;
  private String namespace;
  private Optional<XmlDslConfiguration> xmlDslConfiguration;
  private LicenseModelProperty licenseModelProperty;

  public MuleSdkPluginExtensionModelParser(ArtifactAst ast, TypeLoader typeLoader, ExtensionModelHelper extensionModelHelper) {
    super(ast, typeLoader, extensionModelHelper);
    parseStructure(getExtensionComponentAst(ast));
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
  public LicenseModelProperty getLicenseModelProperty() {
    return licenseModelProperty;
  }

  @Override
  protected Stream<ComponentAst> getTopLevelElements(ArtifactAst ast) {
    return getExtensionComponentAst(ast).directChildrenStream();
  }

  private ComponentAst getExtensionComponentAst(ArtifactAst ast) {
    // At this point we can assume there is only one top level component which is the extension:extension component
    // We don't need to check for this because it should be guaranteed by previous validations
    return ast.topLevelComponents().get(0);
  }

  private void parseStructure(ComponentAst extensionComponentAst) {
    name = getParameter(extensionComponentAst, MULE_SDK_EXTENSION_NAME_PARAMETER_NAME);
    category = Category
        .valueOf(this.<String>getParameter(extensionComponentAst, MULE_SDK_EXTENSION_CATEGORY_PARAMETER_NAME).toUpperCase());
    vendor = getParameter(extensionComponentAst, MULE_SDK_EXTENSION_VENDOR_PARAMETER_NAME);

    parseXmlDslConfiguration(extensionComponentAst);
    parseLicenseModelProperty(extensionComponentAst);

    // use dummy version since this is just for obtaining the namespace
    this.namespace = getExtensionsNamespace(getXmlDslModel(name, "1.0.0", xmlDslConfiguration));
  }

  private void parseXmlDslConfiguration(ComponentAst extensionComponentAst) {
    Optional<String> prefix = getOptionalParameter(extensionComponentAst, MULE_SDK_EXTENSION_PREFIX_PARAMETER_NAME);
    Optional<String> namespace = getOptionalParameter(extensionComponentAst, MULE_SDK_EXTENSION_NAMESPACE_PARAMETER_NAME);
    if (prefix.isPresent() || namespace.isPresent()) {
      xmlDslConfiguration = of(new XmlDslConfiguration(prefix.orElse(""), namespace.orElse("")));
    } else {
      xmlDslConfiguration = empty();
    }
  }

  private void parseLicenseModelProperty(ComponentAst extensionComponentAst) {
    boolean requiresEeLicense =
        getParameter(extensionComponentAst, MULE_SDK_EXTENSION_REQUIRES_ENTERPRISE_LICENSE_PARAMETER_NAME);
    boolean allowsEvaluationLicense = getParameter(extensionComponentAst,
                                                   MULE_SDK_EXTENSION_ALLOWS_EVALUATION_LICENSE_PARAMETER_NAME);
    Optional<String> requiredEntitlement = getOptionalParameter(extensionComponentAst,
                                                                MULE_SDK_EXTENSION_REQUIRED_ENTITLEMENT_PARAMETER_NAME);
    licenseModelProperty = new LicenseModelProperty(requiresEeLicense, allowsEvaluationLicense, requiredEntitlement);
  }
}
