/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.policy.api;


import static java.util.Collections.singletonList;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

/**
 * Defines the {@link XmlNamespaceInfoProvider} for the test policy module
 */
public class TestPolicyXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  public static final String TEST_POLICY_NAMESPACE = "http://www.mulesoft.org/schema/mule/test-policy";
  public static final String TEST_POLICY_PREFIX = "test-policy";

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return singletonList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return TEST_POLICY_NAMESPACE;
      }

      @Override
      public String getNamespace() {
        return TEST_POLICY_PREFIX;
      }
    });
  }
}
