/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.CONFIG_FILE_NAME;
import static org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.NAMESPACE_URI;
import static org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.XML_NODE;

import org.w3c.dom.Node;

public class ComponentCustomAttributeRetrieve {

  private final ComponentModel componentModel;

  /**
   * @param componentModel model from which the custom attribute must be retrieved.
   * @return a handler for retrieving custom attributes.
   */
  public static ComponentCustomAttributeRetrieve from(ComponentModel componentModel) {
    return new ComponentCustomAttributeRetrieve(componentModel);
  }

  private ComponentCustomAttributeRetrieve(ComponentModel componentModel) {
    this.componentModel = componentModel;
  }

  /**
   * @return the namespace URI of the XML source element.
   */
  public String getNamespaceUri() {
    return (String) this.componentModel.getCustomAttributes().get(NAMESPACE_URI);
  }

  /**
   * @return the config file name in which this configuration was defined.
   */
  public String getConfigFileName() {
    return (String) this.componentModel.getCustomAttributes().get(CONFIG_FILE_NAME);
  }

  /**
   * @return the XML node element which was the source of this configuration.
   */
  public Node getNode() {
    return (Node) this.componentModel.getCustomAttributes().get(XML_NODE);
  }
}
