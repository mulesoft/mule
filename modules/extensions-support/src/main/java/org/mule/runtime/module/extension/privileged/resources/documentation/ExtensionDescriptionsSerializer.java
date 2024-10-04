/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.privileged.resources.documentation;

import org.mule.runtime.module.extension.internal.resources.documentation.DefaultExtensionDescriptionsSerializer;

import java.io.InputStream;

public abstract class ExtensionDescriptionsSerializer {

  public static final ExtensionDescriptionsSerializer SERIALIZER = new DefaultExtensionDescriptionsSerializer();

  abstract public String serialize(XmlExtensionDocumentation dto);

  abstract public XmlExtensionDocumentation deserialize(String xml);

  abstract public XmlExtensionDocumentation deserialize(InputStream xml);

  abstract public String getFileName(String extensionName);

}
