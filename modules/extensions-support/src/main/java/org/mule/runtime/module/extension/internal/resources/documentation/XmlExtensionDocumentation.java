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
import javax.xml.bind.annotation.XmlRootElement;

/**
 * a POJO that represents the extension-descriptions.xml file which contains the necessary annotations and setters necessary
 * to allow JAX-B serialization/deserialization.
 *
 * @since 4.0
 */
@XmlRootElement(name = "extension-documentation")
public class XmlExtensionDocumentation {

  private List<XmlExtensionElementDocumentation> elements = new LinkedList<>();

  @XmlElement
  public List<XmlExtensionElementDocumentation> getElements() {
    return elements;
  }

  public void setElements(List<XmlExtensionElementDocumentation> elements) {
    this.elements = elements;
  }
}
