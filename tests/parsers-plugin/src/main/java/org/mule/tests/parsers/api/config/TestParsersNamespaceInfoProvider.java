/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.parsers.api.config;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Arrays;
import java.util.Collection;

public class TestParsersNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  public static final String PARSERS_TEST_NAMESPACE = "parsers-test";

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return Arrays.asList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return "http://www.mulesoft.org/schema/mule/parsers-test";
      }

      @Override
      public String getNamespace() {
        return PARSERS_TEST_NAMESPACE;
      }
    });
  }

}
