/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import org.mule.runtime.api.meta.DescribedObject;
import org.mule.runtime.api.meta.NamedObject;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * a POJO that represents an extension parameter with a name and a description.
 *
 * @since 4.0
 */
public class XmlExtensionParameterDocumentation implements NamedObject, DescribedObject {

  private String name;
  private String description;

  public XmlExtensionParameterDocumentation() {}

  XmlExtensionParameterDocumentation(String name, String description) {
    this.name = name;
    this.description = description;
  }

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
}
