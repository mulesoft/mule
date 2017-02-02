/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_DESCRIPTIONS_FILE_NAME_MASK;
import org.mule.runtime.extension.internal.GenericXmlSerializer;

/**
 * A simple XML JAXB serializer class for {@link XmlExtensionDocumentation}s files.
 *
 * @since 4.0
 */
public class ExtensionDescriptionsSerializer extends GenericXmlSerializer<XmlExtensionDocumentation> {

  public ExtensionDescriptionsSerializer() {
    super(XmlExtensionDocumentation.class);
  }

  public String getFileName(String extensionName) {
    String key = extensionName.replace(" ", "-").toLowerCase();
    return format(EXTENSION_DESCRIPTIONS_FILE_NAME_MASK, key);
  }
}
