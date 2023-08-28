/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import org.mule.runtime.api.meta.DescribedObject;
import org.mule.runtime.api.meta.NamedObject;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * a POJO that represents an extension element with parameters and a description.
 *
 * @since 4.0
 */
public class XmlExtensionElementDocumentation implements NamedObject, DescribedObject {

  private String name;

  private String description;

  private List<XmlExtensionParameterDocumentation> parameters;

  @XmlAttribute
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlElement
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @XmlElementWrapper(name = "parameters")
  @XmlElement(name = "parameter")
  public List<XmlExtensionParameterDocumentation> getParameters() {
    return parameters;
  }

  public void setParameters(List<XmlExtensionParameterDocumentation> parameters) {
    this.parameters = parameters;
  }
}
