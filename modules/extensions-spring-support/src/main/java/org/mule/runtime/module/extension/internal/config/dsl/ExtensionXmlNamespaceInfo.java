/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfo;

/**
 * The {@link XmlNamespaceInfo} for the extension's namespace
 *
 * @since 4.0
 */
public class ExtensionXmlNamespaceInfo implements XmlNamespaceInfo {

  public static final String EXTENSION_NAMESPACE = "extension";

  /**
   * @return {@code http//://www.mulesoft.org/schema/mule/extension}
   */
  @Override
  public String getNamespaceUriPrefix() {
    return "http//://www.mulesoft.org/schema/mule/extension";
  }

  /**
   * @return {@link #EXTENSION_NAMESPACE}
   */
  @Override
  public String getNamespace() {
    return EXTENSION_NAMESPACE;
  }
}
