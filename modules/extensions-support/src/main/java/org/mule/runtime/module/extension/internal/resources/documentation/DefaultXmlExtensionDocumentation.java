/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import org.mule.runtime.module.extension.api.resources.documentation.XmlExtensionDocumentation;

import java.util.LinkedList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * a POJO that represents the extension-descriptions.xml file which contains the necessary annotations and setters necessary to
 * allow JAX-B serialization/deserialization.
 *
 * @since 4.0
 */
@XmlRootElement(name = "extension-documentation")
public class DefaultXmlExtensionDocumentation implements XmlExtensionDocumentation {

  private DefaultXmlExtensionElementDocumentation extension;
  private List<DefaultXmlExtensionElementDocumentation> connections = new LinkedList<>();
  private List<DefaultXmlExtensionElementDocumentation> configs = new LinkedList<>();
  private List<DefaultXmlExtensionElementDocumentation> sources = new LinkedList<>();
  private List<DefaultXmlExtensionElementDocumentation> operation = new LinkedList<>();
  private List<DefaultXmlExtensionElementDocumentation> types = new LinkedList<>();

  @XmlElementWrapper(name = "connections")
  @XmlElement(name = "connection")
  public List<DefaultXmlExtensionElementDocumentation> getConnections() {
    return connections;
  }

  public void setConnections(List<DefaultXmlExtensionElementDocumentation> connections) {
    this.connections = connections;
  }

  @XmlElementWrapper(name = "configs")
  @XmlElement(name = "config")
  public List<DefaultXmlExtensionElementDocumentation> getConfigs() {
    return configs;
  }

  public void setConfigs(List<DefaultXmlExtensionElementDocumentation> configs) {
    this.configs = configs;
  }

  @XmlElementWrapper(name = "sources")
  @XmlElement(name = "source")
  public List<DefaultXmlExtensionElementDocumentation> getSources() {
    return sources;
  }

  public void setSources(List<DefaultXmlExtensionElementDocumentation> sources) {
    this.sources = sources;
  }

  @XmlElementWrapper(name = "operations")
  @XmlElement(name = "operation")
  public List<DefaultXmlExtensionElementDocumentation> getOperations() {
    return operation;
  }

  public void setOperation(List<DefaultXmlExtensionElementDocumentation> operation) {
    this.operation = operation;
  }

  @XmlElementWrapper(name = "types")
  @XmlElement(name = "type")
  public List<DefaultXmlExtensionElementDocumentation> getTypes() {
    return types;
  }

  public void setTypes(List<DefaultXmlExtensionElementDocumentation> types) {
    this.types = types;
  }

  @XmlElement
  public DefaultXmlExtensionElementDocumentation getExtension() {
    return extension;
  }

  public void setExtension(DefaultXmlExtensionElementDocumentation extension) {
    this.extension = extension;
  }
}
