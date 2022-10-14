/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.internal.dsl.DslConstants.THIS_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.THIS_PREFIX;
import static org.mule.sdk.api.annotation.Extension.MULESOFT;

import static java.util.Optional.empty;
import static java.util.Optional.of;

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
 * {@link ExtensionModelParser} implementation for Mule SDK in the context of applications.
 *
 * @since 4.5.0
 */
public class MuleSdkApplicationExtensionModelParser extends MuleSdkExtensionModelParser {

  // The namespace of the extension when it's defined within an application rather than in a separate artifact.
  public static final String APP_LOCAL_EXTENSION_NAMESPACE = "THIS";

  private final String extensionName;

  public MuleSdkApplicationExtensionModelParser(String extensionName,
                                                ArtifactAst ast,
                                                TypeLoader typeLoader,
                                                ExtensionModelHelper extensionModelHelper) {
    super(ast, typeLoader, extensionModelHelper);
    this.extensionName = extensionName;
  }

  @Override
  public String getName() {
    return extensionName;
  }

  @Override
  public Category getCategory() {
    return COMMUNITY;
  }

  @Override
  public String getVendor() {
    return MULESOFT;
  }

  @Override
  public Optional<XmlDslConfiguration> getXmlDslConfiguration() {
    return of(new XmlDslConfiguration(THIS_PREFIX, THIS_NAMESPACE));
  }

  @Override
  public String getNamespace() {
    return APP_LOCAL_EXTENSION_NAMESPACE;
  }

  @Override
  public LicenseModelProperty getLicenseModelProperty() {
    return new LicenseModelProperty(false, true, empty());
  }

  @Override
  protected Stream<ComponentAst> getTopLevelElements(ArtifactAst ast) {
    return ast.topLevelComponentsStream();
  }
}
