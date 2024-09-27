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
public interface XmlExtensionDocumentationApi {

  List<XmlExtensionElementDocumentationApi> getConnections();

  void setConnections(List<XmlExtensionElementDocumentationApi> connections);

  List<XmlExtensionElementDocumentationApi> getConfigs();

  void setConfigs(List<XmlExtensionElementDocumentationApi> configs);

  List<XmlExtensionElementDocumentationApi> getSources();

  void setSources(List<XmlExtensionElementDocumentationApi> sources);

  List<XmlExtensionElementDocumentationApi> getOperations();

  void setOperation(List<XmlExtensionElementDocumentationApi> operations);

  List<XmlExtensionElementDocumentationApi> getTypes();

  void setTypes(List<XmlExtensionElementDocumentationApi> types);

  XmlExtensionElementDocumentationApi getExtension();

  void setExtension(XmlExtensionElementDocumentationApi extension);
}
