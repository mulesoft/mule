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
public abstract class XmlExtensionDocumentation {

  abstract public List<XmlExtensionElementDocumentation> getConnections();

  abstract public void setConnections(List<XmlExtensionElementDocumentation> connections);

  abstract public List<XmlExtensionElementDocumentation> getConfigs();

  abstract public void setConfigs(List<XmlExtensionElementDocumentation> configs);

  abstract public List<XmlExtensionElementDocumentation> getSources();

  abstract public void setSources(List<XmlExtensionElementDocumentation> sources);

  abstract public List<XmlExtensionElementDocumentation> getOperations();

  abstract public void setOperation(List<XmlExtensionElementDocumentation> operations);

  abstract public List<XmlExtensionElementDocumentation> getTypes();

  abstract public void setTypes(List<XmlExtensionElementDocumentation> types);

  abstract public XmlExtensionElementDocumentation getExtension();

  abstract public void setExtension(XmlExtensionElementDocumentation extension);
}
