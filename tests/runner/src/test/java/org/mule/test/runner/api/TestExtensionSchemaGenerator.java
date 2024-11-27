/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.api;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator;

/**
 * {@link ExtensionSchemaGenerator} test implementation in order to test {@link ExtensionPluginMetadataGenerator}.
 */
public class TestExtensionSchemaGenerator implements ExtensionSchemaGenerator {

  @Override
  public String generate(ExtensionModel extensionModel, DslResolvingContext context) {
    // Simple test XSD
    return """
        <?xml version="1.0" encoding="UTF-8" ?>
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">

        <xs:element name="shiporder">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="orderperson" type="xs:string"/>
              <xs:element name="shipto">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="name" type="xs:string"/>
                    <xs:element name="address" type="xs:string"/>
                    <xs:element name="city" type="xs:string"/>
                    <xs:element name="country" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:sequence>
            <xs:attribute name="orderid" type="xs:string" use="required"/>
          </xs:complexType>
        </xs:element>

        </xs:schema>""";
  }

  @Override
  public String generate(ExtensionModel extensionModel, DslResolvingContext context, DslSyntaxResolver dsl) {
    return generate(extensionModel, context);
  }
}
