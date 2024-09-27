/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import org.mule.runtime.api.meta.DescribedObject;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.module.extension.privileged.resources.documentation.XmlExtensionElementDocumentation;
import org.mule.runtime.module.extension.privileged.resources.documentation.XmlExtensionParameterDocumentation;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

/**
 * a POJO that represents an extension element with parameters and a description.
 *
 * @since 4.0
 */
public class DefaultXmlExtensionElementDocumentation implements NamedObject, DescribedObject, XmlExtensionElementDocumentation {

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
