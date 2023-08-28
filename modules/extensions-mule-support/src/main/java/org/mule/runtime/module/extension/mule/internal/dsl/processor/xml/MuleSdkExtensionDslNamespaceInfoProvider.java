/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.dsl.processor.xml;

import static org.mule.runtime.extension.internal.dsl.xml.XmlDslConstants.MULE_SDK_EXTENSION_DSL_NAMESPACE;
import static org.mule.runtime.extension.internal.dsl.xml.XmlDslConstants.MULE_SDK_EXTENSION_DSL_NAMESPACE_URI;

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

  private static final Collection<XmlNamespaceInfo> XML_NAMESPACE_INFO =
      singletonList(new StaticXmlNamespaceInfo(MULE_SDK_EXTENSION_DSL_NAMESPACE_URI,
                                               MULE_SDK_EXTENSION_DSL_NAMESPACE));

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return XML_NAMESPACE_INFO;
  }
}
