/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.utils;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.extension.api.util.XmlModelUtils.createXmlLanguageModel;

import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.parser.XmlDslConfiguration;

import java.util.Optional;

public class JavaModelLoaderUtils {

  /**
   * Utility method to obtain the {@link XmlDslModel} of a given {@link XmlDslConfiguration}
   *
   * @param extensionElement              the extension element
   * @param version                       version of the extension
   * @param xmlDslAnnotationConfiguration configuration of {@link org.mule.runtime.extension.api.annotation.dsl.xml.Xml} of
   *                                      {@link org.mule.sdk.api.annotation.dsl.xml.Xml}
   * @return the {@link XmlDslModel}
   */
  public static XmlDslModel getXmlDslModel(ExtensionElement extensionElement, String version,
                                           Optional<XmlDslConfiguration> xmlDslAnnotationConfiguration) {
    String extensionName = extensionElement.getName();
    Optional<String> prefix = empty();
    Optional<String> namespace = empty();

    if (xmlDslAnnotationConfiguration.isPresent()) {
      prefix = of(xmlDslAnnotationConfiguration.get().getPrefix());
      namespace = of(xmlDslAnnotationConfiguration.get().getNamespace());
    }

    return createXmlLanguageModel(prefix, namespace, extensionName, version);
  }
}
