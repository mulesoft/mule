/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.dsl.processor.xml.provider;

import static java.util.Arrays.asList;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

/**
 * Provides the core namespace XML information.
 *
 * @since 4.0
 */
public class CoreXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return asList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return "http://www.mulesoft.org/schema/mule/core";
      }

      @Override
      public String getNamespace() {
        return CORE_PREFIX;
      }
    });
  }
}
