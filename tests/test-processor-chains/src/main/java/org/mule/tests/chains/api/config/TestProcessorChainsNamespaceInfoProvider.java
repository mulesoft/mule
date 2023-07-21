/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tests.chains.api.config;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Arrays;
import java.util.Collection;

public class TestProcessorChainsNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  public static final String TEST_PROCESSOR_CHAINS_NAMESPACE = "test-processor-chains";

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return Arrays.asList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return "http://www.mulesoft.org/schema/mule/test-processor-chains";
      }

      @Override
      public String getNamespace() {
        return TEST_PROCESSOR_CHAINS_NAMESPACE;
      }
    });
  }

}
