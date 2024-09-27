/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.privileged.resources.documentation;

import org.mule.runtime.module.extension.internal.resources.documentation.ExtensionDescriptionsSerializer;

import java.io.InputStream;

public interface ExtensionDescriptionsSerializerApi {

  String serialize(XmlExtensionDocumentationApi dto);

  XmlExtensionDocumentationApi deserialize(String xml);

  XmlExtensionDocumentationApi deserialize(InputStream xml);

  String getFileName(String extensionName);

  static ExtensionDescriptionsSerializerApi getSerializer() {
    return ExtensionDescriptionsSerializer.getExtensionDescriptionsSerializer();
  }

}
