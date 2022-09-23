/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.metadata;

import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.XmlDslConfiguration;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkExtensionModelParser;

import java.util.Optional;

/**
 * Parses the syntactic definition of the metadata associated with an {@link ExtensionModel}.
 * <p>
 * It is meant to be used as a helper for {@link MuleSdkExtensionModelParser}.
 *
 * @since 4.5.0
 */
public interface MuleSdkExtensionModelMetadataParser {

  /**
   * @return the extension's name
   */
  String getName();

  /**
   * @return the extension's {@link Category}
   */
  Category getCategory();

  /**
   * @return the extension's vendor
   */
  String getVendor();

  /**
   * @return the extension's {@link XmlDslModel}
   */
  Optional<XmlDslConfiguration> getXmlDslConfiguration();

  /**
   * @return the extension's namespace.
   */
  String getNamespace();

  /**
   * @return a {@link LicenseModelProperty} which describes the extension's licensing.
   */
  LicenseModelProperty getLicenseModelProperty();
}
