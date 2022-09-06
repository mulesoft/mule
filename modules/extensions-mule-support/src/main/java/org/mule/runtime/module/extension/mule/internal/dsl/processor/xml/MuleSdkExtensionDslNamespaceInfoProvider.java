/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.dsl.processor.xml;

import static org.mule.runtime.extension.api.util.XmlModelUtils.buildSchemaLocation;
import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

import org.mule.runtime.ast.internal.xml.StaticXmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

/**
 * {@link XmlNamespaceInfoProvider} for the {@code extension} namespace
 *
 * @since 4.5.0
 */
public class MuleSdkExtensionDslNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  public static final String MULE_EXTENSION_DSL_NAMESPACE = "extension";
  public static final String MULE_EXTENSION_DSL_NAMESPACE_URI = format(DEFAULT_NAMESPACE_URI_MASK, "mule-extension");
  public static final String MULE_EXTENSION_DSL_XSD_FILE_NAME = "mule-extension.xsd";
  public static final String MULE_EXTENSION_DSL_SCHEMA_LOCATION =
      buildSchemaLocation(MULE_EXTENSION_DSL_NAMESPACE, MULE_EXTENSION_DSL_XSD_FILE_NAME);

  private static final Collection<XmlNamespaceInfo> XML_NAMESPACE_INFO =
      singletonList(new StaticXmlNamespaceInfo(MULE_EXTENSION_DSL_NAMESPACE_URI, MULE_EXTENSION_DSL_NAMESPACE));

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return XML_NAMESPACE_INFO;
  }
}
