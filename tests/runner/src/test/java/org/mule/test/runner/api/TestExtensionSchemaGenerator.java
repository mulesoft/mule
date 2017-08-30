/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator;

/**
 * {@link ExtensionSchemaGenerator} test implementation in order to test {@link ExtensionPluginMetadataGenerator}.
 */
public class TestExtensionSchemaGenerator implements ExtensionSchemaGenerator {

  @Override
  public String generate(ExtensionModel extensionModel, DslResolvingContext context) {
    // Simple test XSD
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
        + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
        + "\n"
        + "<xs:element name=\"shiporder\">\n"
        + "  <xs:complexType>\n"
        + "    <xs:sequence>\n"
        + "      <xs:element name=\"orderperson\" type=\"xs:string\"/>\n"
        + "      <xs:element name=\"shipto\">\n"
        + "        <xs:complexType>\n"
        + "          <xs:sequence>\n"
        + "            <xs:element name=\"name\" type=\"xs:string\"/>\n"
        + "            <xs:element name=\"address\" type=\"xs:string\"/>\n"
        + "            <xs:element name=\"city\" type=\"xs:string\"/>\n"
        + "            <xs:element name=\"country\" type=\"xs:string\"/>\n"
        + "          </xs:sequence>\n"
        + "        </xs:complexType>\n"
        + "      </xs:element>\n"
        + "    </xs:sequence>\n"
        + "    <xs:attribute name=\"orderid\" type=\"xs:string\" use=\"required\"/>\n"
        + "  </xs:complexType>\n"
        + "</xs:element>\n"
        + "\n"
        + "</xs:schema>";
  }
}
