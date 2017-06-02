/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * a POJO that represents the extension-descriptions.xml file which contains the necessary annotations and setters necessary
 * to allow JAX-B serialization/deserialization.
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

  @XmlElement
  public XmlExtensionElementDocumentation getExtension() {
    return extension;
  }

  public void setExtension(XmlExtensionElementDocumentation extension) {
    this.extension = extension;
  }
}
