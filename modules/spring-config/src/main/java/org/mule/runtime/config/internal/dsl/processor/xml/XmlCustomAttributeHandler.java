/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.processor.xml;

import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.api.dsl.processor.ConfigLine;

import org.w3c.dom.Node;

/**
 * Handler for adding and removing custom XML attributes from and to {@code ConfigLine} and {@code ComponentModel}.
 *
 * @since 4.0
 */
public class XmlCustomAttributeHandler {

  public static final String IS_CDATA = "IS_CDATA";
  public static final String DECLARED_PREFIX = "DECLARED_PREFIX";
  public static final String NAMESPACE_URI = "NAMESPACE_URI";
  public static final String XML_NODE = "XML_NODE";
  public static final String LINE_NUMBER = "LINE_NUMBER";
  public static final String CONFIG_FILE_NAME = "CONFIG_FILE_NAME";

  /**
   * @param builder builder which is going to be used to create the
   *        {@code org.mule.runtime.config.dsl.processor.ConfigLine}.
   * @return handler for adding custom attributes to the builder.
   */
  public static ConfigLineCustomAttributeStore to(ConfigLine.Builder builder) {
    return new ConfigLineCustomAttributeStore(builder);
  }

  /**
   * @param configLine line from which the custom attribute must be retrieved.
   * @return a handler for retrieving custom attributes.
   */
  public static ConfigLineCustomAttributeRetrieve from(ConfigLine configLine) {
    return new ConfigLineCustomAttributeRetrieve(configLine);
  }

  /**
   * @param builder builder which is going to be used to create the
   *        {@code org.mule.runtime.config.dsl.processor.ComponentModel}.
   * @return handler for adding custom attributes to the builder.
   */
  public static ComponentCustomAttributeStore to(ComponentModel.Builder builder) {
    return new ComponentCustomAttributeStore(builder);
  }

  /**
   * @param componentModel model from which the custom attribute must be retrieved.
   * @return a handler for retrieving custom attributes.
   */
  public static ComponentCustomAttributeRetrieve from(ComponentModel componentModel) {
    return new ComponentCustomAttributeRetrieve(componentModel);
  }

  public static class ConfigLineCustomAttributeStore {

    private final ConfigLine.Builder builder;

    private ConfigLineCustomAttributeStore(ConfigLine.Builder builder) {
      this.builder = builder;
    }

    public ConfigLineCustomAttributeStore addNode(Node node) {
      addCustomAttributes(node);
      return this;
    }

    private void addCustomAttributes(Node node) {
      this.builder.addCustomAttribute(XML_NODE, node);
      this.builder.addCustomAttribute(NAMESPACE_URI, node.getNamespaceURI());
      if (node.getPrefix() != null) {
        this.builder.addCustomAttribute(DECLARED_PREFIX, node.getPrefix());
      }
    }
  }

  public static class ConfigLineCustomAttributeRetrieve {

    private final ConfigLine configLine;

    private ConfigLineCustomAttributeRetrieve(ConfigLine configLine) {
      this.configLine = configLine;
    }

    public Node getNode() {
      return (Node) this.configLine.getCustomAttributes().get(XML_NODE);
    }

  }

  public static class ComponentCustomAttributeStore {

    private final ComponentModel.Builder builder;

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

  public static class ComponentCustomAttributeRetrieve {

    private final ComponentModel componentModel;

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

}
