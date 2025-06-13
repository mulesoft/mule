/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.THIS_NAMESPACE;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.THIS_PREFIX;
import static org.mule.sdk.api.annotation.Extension.MULESOFT;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.model.ExtensionModelHelper;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParser;
import org.mule.runtime.extension.api.loader.parser.LicensingParser;
import org.mule.runtime.extension.api.loader.parser.MinMuleVersionParser;
import org.mule.runtime.extension.api.loader.parser.XmlDslConfiguration;

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
  private static final String MIN_MULE_VERSION = "4.5";

  private final String extensionName;

  public MuleSdkApplicationExtensionModelParser(String extensionName,
                                                ArtifactAst ast,
                                                TypeLoader typeLoader,
                                                ExtensionModelHelper extensionModelHelper) {
    super(typeLoader, extensionModelHelper);
    this.extensionName = extensionName;
    init(ast);
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
  public Optional<MinMuleVersionParser> getResolvedMinMuleVersion() {
    return of(new MuleSdkMinMuleVersionParser(format("Application %s has min mule version %s because the Mule Sdk was introduced in that version.",
                                                     extensionName, MIN_MULE_VERSION)));
  }

  @Override
  public LicensingParser getLicensingParser() {
    return new MuleSdkLicensingParser(false, true, empty());
  }

  @Override
  protected Stream<ComponentAst> getTopLevelElements(ArtifactAst ast) {
    return ast.topLevelComponentsStream();
  }
}
