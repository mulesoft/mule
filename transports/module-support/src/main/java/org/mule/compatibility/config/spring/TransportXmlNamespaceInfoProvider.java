/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring;

import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Arrays;
import java.util.Collection;

/**
 * Provides the core transport namespace XML information.
 *
 * @since 4.0
 */
public class TransportXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  public static final String TRANSPORTS_NAMESPACE_NAME = "transports";

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return Arrays.asList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return "http://www.mulesoft.org/schema/mule/transports";
      }

      @Override
      public String getNamespace() {
        return TRANSPORTS_NAMESPACE_NAME;
      }
    });
  }
}
