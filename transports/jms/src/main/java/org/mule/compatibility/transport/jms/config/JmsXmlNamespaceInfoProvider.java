/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.config;

import static java.util.Arrays.asList;
import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

/**
 * {@link XmlNamespaceInfoProvider} for JMS transport.
 *
 * @since 4.0
 */
public class JmsXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  public static final String JMS_NAMESPACE = "jms";

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return asList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return "http://www.mulesoft.org/schema/mule/transport/jms";
      }

      @Override
      public String getNamespace() {
        return JMS_NAMESPACE;
      }
    });
  }
}
