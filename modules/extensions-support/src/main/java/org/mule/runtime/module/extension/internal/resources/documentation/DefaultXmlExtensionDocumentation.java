/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import org.mule.runtime.module.extension.privileged.resources.documentation.XmlExtensionDocumentation;
import org.mule.runtime.module.extension.privileged.resources.documentation.XmlExtensionElementDocumentation;

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
public class DefaultXmlExtensionDocumentation extends XmlExtensionDocumentation {

  private XmlExtensionElementDocumentation extension;
  private List<XmlExtensionElementDocumentation> connections = new LinkedList<>();
  private List<XmlExtensionElementDocumentation> configs = new LinkedList<>();
  private List<XmlExtensionElementDocumentation> sources = new LinkedList<>();
  private List<XmlExtensionElementDocumentation> operation = new LinkedList<>();
  private List<XmlExtensionElementDocumentation> types = new LinkedList<>();

  @XmlElementWrapper(name = "connections")
  @XmlElement(name = "connection", type = DefaultXmlExtensionElementDocumentation.class)
  public List<XmlExtensionElementDocumentation> getConnections() {
    return connections;
  }

  public void setConnections(List<XmlExtensionElementDocumentation> connections) {
    this.connections = connections;
  }

  @XmlElementWrapper(name = "configs")
  @XmlElement(name = "config", type = DefaultXmlExtensionElementDocumentation.class)
  public List<XmlExtensionElementDocumentation> getConfigs() {
    return configs;
  }

  public void setConfigs(List<XmlExtensionElementDocumentation> configs) {
    this.configs = configs;
  }

  @XmlElementWrapper(name = "sources")
  @XmlElement(name = "source", type = DefaultXmlExtensionElementDocumentation.class)
  public List<XmlExtensionElementDocumentation> getSources() {
    return sources;
  }

  public void setSources(List<XmlExtensionElementDocumentation> sources) {
    this.sources = sources;
  }

  @XmlElementWrapper(name = "operations")
  @XmlElement(name = "operation", type = DefaultXmlExtensionElementDocumentation.class)
  public List<XmlExtensionElementDocumentation> getOperations() {
    return operation;
  }

  public void setOperation(List<XmlExtensionElementDocumentation> operation) {
    this.operation = operation;
  }

  @XmlElementWrapper(name = "types")
  @XmlElement(name = "type", type = DefaultXmlExtensionElementDocumentation.class)
  public List<XmlExtensionElementDocumentation> getTypes() {
    return types;
  }

  public void setTypes(List<XmlExtensionElementDocumentation> types) {
    this.types = types;
  }

  @XmlElement(type = DefaultXmlExtensionElementDocumentation.class)
  public XmlExtensionElementDocumentation getExtension() {
    return extension;
  }

  public void setExtension(XmlExtensionElementDocumentation extension) {
    this.extension = extension;
  }
}
