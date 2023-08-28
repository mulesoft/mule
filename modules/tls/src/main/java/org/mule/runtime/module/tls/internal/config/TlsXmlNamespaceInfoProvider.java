/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tls.internal.config;

import static java.util.Arrays.asList;
import static org.mule.runtime.module.tls.internal.config.TlsComponentBuildingDefinitionProvider.TLS_NAMESPACE;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

/**
 * Provides the TLS namespace XML information.
 *
 * @since 4.0
 */
public class TlsXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return asList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return "http://www.mulesoft.org/schema/mule/tls";
      }

      @Override
      public String getNamespace() {
        return TLS_NAMESPACE;
      }
    });
  }
}
