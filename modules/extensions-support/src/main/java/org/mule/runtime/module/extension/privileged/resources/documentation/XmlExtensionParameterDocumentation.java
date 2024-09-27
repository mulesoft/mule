/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.privileged.resources.documentation;

/**
 * a POJO that represents an extension parameter with a name and a description.
 *
 * @since 4.0
 */
public interface XmlExtensionParameterDocumentation {

  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String description);
}
