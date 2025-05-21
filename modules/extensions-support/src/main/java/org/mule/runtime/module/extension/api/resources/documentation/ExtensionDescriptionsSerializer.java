/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.resources.documentation;

import org.mule.runtime.module.extension.internal.resources.documentation.DefaultExtensionDescriptionsSerializer;

import java.io.InputStream;

/**
 * A simple XML JAXB serializer class for {@link XmlExtensionDocumentation}s files.
 *
 * @since 4.0
 */
public interface ExtensionDescriptionsSerializer {

  ExtensionDescriptionsSerializer SERIALIZER = new DefaultExtensionDescriptionsSerializer();

  String serialize(XmlExtensionDocumentation dto) throws ExtensionDescriptionSerializerException;

  XmlExtensionDocumentation deserialize(String xml) throws ExtensionDescriptionSerializerException;

  XmlExtensionDocumentation deserialize(InputStream xml) throws ExtensionDescriptionSerializerException;

  String getFileName(String extensionName);

}
