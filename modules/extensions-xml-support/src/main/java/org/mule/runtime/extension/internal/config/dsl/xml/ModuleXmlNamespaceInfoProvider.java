/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl.xml;

import static java.util.Collections.singletonList;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

public class ModuleXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  public static final String MODULE_DSL_NAMESPACE_URI = "http://www.mulesoft.org/schema/mule/module";
  public static final String MODULE_DSL_NAMESPACE = "module";
  public static final String MODULE_ROOT_NODE_NAME = "module";

  private static final Collection<XmlNamespaceInfo> XML_NAMESPACE_INFO =
      singletonList(new XmlNamespaceInfo() {

        @Override
        public String getNamespaceUriPrefix() {
          return MODULE_DSL_NAMESPACE_URI;
        }

        @Override
        public String getNamespace() {
          return MODULE_DSL_NAMESPACE;
        }
      });

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return XML_NAMESPACE_INFO;
  }
}
