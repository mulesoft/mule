/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;


import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.util.XmlModelUtils.createXmlLanguageModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;

import java.util.Optional;

/**
 * Verifies if the extension is annotated with {@link Xml} and if so, enriches the {@link ExtensionDeclarer} with a
 * {@link XmlDslModel}.
 * <p>
 * To get a hold of the {@link Class} on which the {@link Xml} annotation is expected to be, the {@link ExtensionLoadingContext} will be
 * queried for such a model property. If such property is not present, then this enricher will return without any side effects
 *
 * @since 4.0
 */
public final class JavaXmlDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    Xml xml = extractAnnotation(extensionLoadingContext.getExtensionDeclarer().getDeclaration(), Xml.class);
    ExtensionDeclarer declarer = extensionLoadingContext.getExtensionDeclarer();
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    declarer.withXmlDsl(getXmlLanguageModel(xml, extensionDeclaration));
  }

  private XmlDslModel getXmlLanguageModel(Xml xml, ExtensionDeclaration extensionDeclaration) {
    final Optional<String> extensionNamespace = xml != null ? ofNullable(xml.prefix()) : empty();
    final Optional<String> extensionNamespaceLocation = xml != null ? ofNullable(xml.namespace()) : empty();
    return createXmlLanguageModel(extensionNamespace, extensionNamespaceLocation, extensionDeclaration.getName(),
                                  extensionDeclaration.getVersion());
  }
}
