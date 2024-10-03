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

  abstract public List<? extends XmlExtensionElementDocumentation> getConnections();

  abstract public List<? extends XmlExtensionElementDocumentation> getConfigs();

  abstract public List<? extends XmlExtensionElementDocumentation> getSources();

  abstract public List<? extends XmlExtensionElementDocumentation> getOperations();

  abstract public List<? extends XmlExtensionElementDocumentation> getTypes();

  abstract public XmlExtensionElementDocumentation getExtension();
}
