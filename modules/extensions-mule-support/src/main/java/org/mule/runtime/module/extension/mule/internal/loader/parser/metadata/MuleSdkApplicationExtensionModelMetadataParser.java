/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.metadata;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.internal.dsl.DslConstants.THIS_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.THIS_PREFIX;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkExtensionModelParser.APP_LOCAL_EXTENSION_NAMESPACE;
import static org.mule.sdk.api.annotation.Extension.MULESOFT;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.meta.Category;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.XmlDslConfiguration;

import java.util.Optional;

/**
 * {@link MuleSdkExtensionModelMetadataParser} implementation for Mule SDK extensions defined by applications.
 *
 * @since 4.5.0
 */
public class MuleSdkApplicationExtensionModelMetadataParser implements MuleSdkExtensionModelMetadataParser {

  private final String extensionName;

  public MuleSdkApplicationExtensionModelMetadataParser(String extensionName) {
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
}
