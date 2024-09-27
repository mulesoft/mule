/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import org.mule.runtime.module.extension.privileged.resources.documentation.XmlExtensionDocumentationApi;
import org.mule.runtime.module.extension.privileged.resources.documentation.XmlExtensionElementDocumentationApi;

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
public class XmlExtensionDocumentation implements XmlExtensionDocumentationApi {

  private XmlExtensionElementDocumentationApi extension;
  private List<XmlExtensionElementDocumentationApi> connections = new LinkedList<>();
  private List<XmlExtensionElementDocumentationApi> configs = new LinkedList<>();
  private List<XmlExtensionElementDocumentationApi> sources = new LinkedList<>();
  private List<XmlExtensionElementDocumentationApi> operation = new LinkedList<>();
  private List<XmlExtensionElementDocumentationApi> types = new LinkedList<>();

  @XmlElementWrapper(name = "connections")
  @XmlElement(name = "connection")
  public List<XmlExtensionElementDocumentationApi> getConnections() {
    return connections;
  }

  public void setConnections(List<XmlExtensionElementDocumentationApi> connections) {
    this.connections = connections;
  }

  @XmlElementWrapper(name = "configs")
  @XmlElement(name = "config")
  public List<XmlExtensionElementDocumentationApi> getConfigs() {
    return configs;
  }

  public void setConfigs(List<XmlExtensionElementDocumentationApi> configs) {
    this.configs = configs;
  }

  @XmlElementWrapper(name = "sources")
  @XmlElement(name = "source")
  public List<XmlExtensionElementDocumentationApi> getSources() {
    return sources;
  }

  public void setSources(List<XmlExtensionElementDocumentationApi> sources) {
    this.sources = sources;
  }

  @XmlElementWrapper(name = "operations")
  @XmlElement(name = "operation")
  public List<XmlExtensionElementDocumentationApi> getOperations() {
    return operation;
  }

  public void setOperation(List<XmlExtensionElementDocumentationApi> operation) {
    this.operation = operation;
  }

  @XmlElementWrapper(name = "types")
  @XmlElement(name = "type")
  public List<XmlExtensionElementDocumentationApi> getTypes() {
    return types;
  }

  public void setTypes(List<XmlExtensionElementDocumentationApi> types) {
    this.types = types;
  }

  @XmlElement
  public XmlExtensionElementDocumentationApi getExtension() {
    return extension;
  }

  public void setExtension(XmlExtensionElementDocumentationApi extension) {
    this.extension = extension;
  }
}
