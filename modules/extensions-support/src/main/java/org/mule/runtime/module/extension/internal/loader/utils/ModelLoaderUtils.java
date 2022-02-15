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
import org.mule.runtime.api.meta.model.declaration.fluent.WithSemanticTermsDeclaration;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.parser.SemanticTermsParser;
import org.mule.runtime.module.extension.internal.loader.parser.XmlDslConfiguration;
import org.mule.sdk.api.annotation.dsl.xml.Xml;

import java.util.Optional;

/**
 * Utility class for {@link ModelLoaderDelegate model loaders}
 *
 * @since 4.0
 */
public final class ModelLoaderUtils {

  private ModelLoaderUtils() {}

  /**
   * Adds all the semantic terms in the {@code parser} into the given {@code declaration}
   *
   * @param declaration a declaration
   * @param parser      a parser
   * @since 4.5.0
   */
  public static void addSemanticTerms(WithSemanticTermsDeclaration declaration, SemanticTermsParser parser) {
    declaration.getSemanticTerms().addAll(parser.getSemanticTerms());
  }

  /**
   * Utility method to obtain a default {@link XmlDslModel} of a given {@link XmlDslConfiguration}
   *
   * @param extensionName                 the name of the extension
   * @param version                       version of the extension
   * @param xmlDslAnnotationConfiguration configuration of {@link org.mule.runtime.extension.api.annotation.dsl.xml.Xml} of
   *                                      {@link Xml}
   * @return the {@link XmlDslModel}
   * @since 4.5.0
   */
  public static XmlDslModel getXmlDslModel(String extensionName,
                                           String version,
                                           Optional<XmlDslConfiguration> xmlDslAnnotationConfiguration) {
    Optional<String> prefix = empty();
    Optional<String> namespace = empty();

    if (xmlDslAnnotationConfiguration.isPresent()) {
      prefix = of(xmlDslAnnotationConfiguration.get().getPrefix());
      namespace = of(xmlDslAnnotationConfiguration.get().getNamespace());
    }

    return createXmlLanguageModel(prefix, namespace, extensionName, version);
  }

  /**
   * Utility method to obtain a default {@link XmlDslModel} of a given {@link XmlDslConfiguration}
   *
   * @param extensionElement              the extension element
   * @param version                       version of the extension
   * @param xmlDslAnnotationConfiguration configuration of {@link org.mule.runtime.extension.api.annotation.dsl.xml.Xml} of
   *                                      {@link Xml}
   * @return the {@link XmlDslModel}
   * @since 4.5.0
   */
  public static XmlDslModel getXmlDslModel(ExtensionElement extensionElement,
                                           String version,
                                           Optional<XmlDslConfiguration> xmlDslAnnotationConfiguration) {
    return getXmlDslModel(extensionElement.getName(), version, xmlDslAnnotationConfiguration);
  }
}
