/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.resources.documentation;

import java.util.List;

/**
 * a POJO that represents the extension-descriptions.xml file which contains the necessary annotations and setters necessary to
 * allow JAX-B serialization/deserialization.
 *
 * @since 4.0
 */
public interface XmlExtensionDocumentation {

  List<? extends XmlExtensionElementDocumentation> getConnections();

  List<? extends XmlExtensionElementDocumentation> getConfigs();

  List<? extends XmlExtensionElementDocumentation> getSources();

  List<? extends XmlExtensionElementDocumentation> getOperations();

  List<? extends XmlExtensionElementDocumentation> getTypes();

  XmlExtensionElementDocumentation getExtension();
}
