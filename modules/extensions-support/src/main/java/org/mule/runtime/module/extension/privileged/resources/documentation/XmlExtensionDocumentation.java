/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.privileged.resources.documentation;

import java.util.List;

/**
 * a POJO that represents the extension-descriptions.xml file which contains the necessary annotations and setters necessary to
 * allow JAX-B serialization/deserialization.
 *
 * @since 4.0
 */
public interface XmlExtensionDocumentation {

  List<XmlExtensionElementDocumentation> getConnections();

  void setConnections(List<XmlExtensionElementDocumentation> connections);

  List<XmlExtensionElementDocumentation> getConfigs();

  void setConfigs(List<XmlExtensionElementDocumentation> configs);

  List<XmlExtensionElementDocumentation> getSources();

  void setSources(List<XmlExtensionElementDocumentation> sources);

  List<XmlExtensionElementDocumentation> getOperations();

  void setOperation(List<XmlExtensionElementDocumentation> operations);

  List<XmlExtensionElementDocumentation> getTypes();

  void setTypes(List<XmlExtensionElementDocumentation> types);

  XmlExtensionElementDocumentation getExtension();

  void setExtension(XmlExtensionElementDocumentation extension);
}
