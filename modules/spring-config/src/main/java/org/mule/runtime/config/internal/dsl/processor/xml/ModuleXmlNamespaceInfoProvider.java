/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.processor.xml;

import static org.mule.runtime.extension.internal.dsl.xml.XmlDslConstants.MODULE_DSL_NAMESPACE;
import static org.mule.runtime.extension.internal.dsl.xml.XmlDslConstants.MODULE_DSL_NAMESPACE_URI;

import static java.util.Collections.singletonList;

import org.mule.runtime.ast.internal.xml.StaticXmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

public class ModuleXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  private static final Collection<XmlNamespaceInfo> XML_NAMESPACE_INFO =
      singletonList(new StaticXmlNamespaceInfo(MODULE_DSL_NAMESPACE_URI, MODULE_DSL_NAMESPACE));

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return XML_NAMESPACE_INFO;
  }
}
