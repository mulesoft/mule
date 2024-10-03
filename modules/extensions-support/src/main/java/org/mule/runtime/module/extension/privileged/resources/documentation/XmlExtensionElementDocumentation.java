/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.privileged.resources.documentation;

import java.util.List;

public abstract class XmlExtensionElementDocumentation {

  abstract public String getName();

  abstract public void setName(String name);

  abstract public String getDescription();

  abstract public void setDescription(String description);

  abstract public List<? extends XmlExtensionParameterDocumentation> getParameters();
}
