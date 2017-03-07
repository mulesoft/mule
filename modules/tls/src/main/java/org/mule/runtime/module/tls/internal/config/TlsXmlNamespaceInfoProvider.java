/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
