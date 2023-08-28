/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * a POJO that represents the extension-descriptions.xml file which contains the necessary annotations and setters necessary to
 * allow JAX-B serialization/deserialization.
 *
 * @since 4.0
 */
@XmlRootElement(name = "extension-documentation")
public class XmlExtensionDocumentation {

  private XmlExtensionElementDocumentation extension;
  private List<XmlExtensionElementDocumentation> connections = new LinkedList<>();
  private List<XmlExtensionElementDocumentation> configs = new LinkedList<>();
  private List<XmlExtensionElementDocumentation> sources = new LinkedList<>();
  private List<XmlExtensionElementDocumentation> operation = new LinkedList<>();
  private List<XmlExtensionElementDocumentation> types = new LinkedList<>();

  @XmlElementWrapper(name = "connections")
  @XmlElement(name = "connection")
  public List<XmlExtensionElementDocumentation> getConnections() {
    return connections;
  }

  public void setConnections(List<XmlExtensionElementDocumentation> connections) {
    this.connections = connections;
  }

  @XmlElementWrapper(name = "configs")
  @XmlElement(name = "config")
  public List<XmlExtensionElementDocumentation> getConfigs() {
    return configs;
  }

  public void setConfigs(List<XmlExtensionElementDocumentation> configs) {
    this.configs = configs;
  }

  @XmlElementWrapper(name = "sources")
  @XmlElement(name = "source")
  public List<XmlExtensionElementDocumentation> getSources() {
    return sources;
  }

  public void setSources(List<XmlExtensionElementDocumentation> sources) {
    this.sources = sources;
  }

  @XmlElementWrapper(name = "operations")
  @XmlElement(name = "operation")
  public List<XmlExtensionElementDocumentation> getOperations() {
    return operation;
  }

  public void setOperation(List<XmlExtensionElementDocumentation> operation) {
    this.operation = operation;
  }

  @XmlElementWrapper(name = "types")
  @XmlElement(name = "type")
  public List<XmlExtensionElementDocumentation> getTypes() {
    return types;
  }

  public void setTypes(List<XmlExtensionElementDocumentation> types) {
    this.types = types;
  }

  @XmlElement
  public XmlExtensionElementDocumentation getExtension() {
    return extension;
  }

  public void setExtension(XmlExtensionElementDocumentation extension) {
    this.extension = extension;
  }
}
