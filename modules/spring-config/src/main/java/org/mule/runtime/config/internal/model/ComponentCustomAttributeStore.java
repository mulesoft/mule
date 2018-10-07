/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.DECLARED_PREFIX;
import static org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.NAMESPACE_URI;
import static org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.XML_NODE;

import org.w3c.dom.Node;

public class ComponentCustomAttributeStore {

  private final ComponentModel.Builder builder;

  /**
   * @param builder builder which is going to be used to create the {@code org.mule.runtime.config.dsl.processor.ComponentModel}.
   * @return handler for adding custom attributes to the builder.
   */
  public static ComponentCustomAttributeStore to(ComponentModel.Builder builder) {
    return new ComponentCustomAttributeStore(builder);
  }

  private ComponentCustomAttributeStore(ComponentModel.Builder builder) {
    this.builder = builder;
  }

  /**
   * @param node XML source element of the model.
   * @return the store.
   */
  public ComponentCustomAttributeStore addNode(Node node) {
    this.builder.addCustomAttribute(XML_NODE, node);
    this.builder.addCustomAttribute(NAMESPACE_URI, node.getNamespaceURI());
    this.builder.addCustomAttribute(NAMESPACE_URI, node.getNamespaceURI());
    if (node.getPrefix() != null) {
      this.builder.addCustomAttribute(DECLARED_PREFIX, node.getPrefix());
    }
    return this;
  }

}


