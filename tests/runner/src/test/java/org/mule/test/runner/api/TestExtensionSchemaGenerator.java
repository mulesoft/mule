/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
