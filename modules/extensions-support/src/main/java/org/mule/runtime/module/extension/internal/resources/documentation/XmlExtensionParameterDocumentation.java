/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
