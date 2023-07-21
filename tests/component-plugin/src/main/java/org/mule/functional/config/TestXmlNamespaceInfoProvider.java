/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.config;

import static java.util.Arrays.asList;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

/**
 * {@link XmlNamespaceInfoProvider} for TEST module.
 *
 * @since 4.0
 */
public class TestXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  public static final String TEST_NAMESPACE = "test";

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return asList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return "http://www.mulesoft.org/schema/mule/test";
      }

      @Override
      public String getNamespace() {
        return TEST_NAMESPACE;
      }
    });
  }
}
