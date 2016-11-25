/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;


import static org.mule.runtime.extension.xml.dsl.api.XmlModelUtils.createXmlLanguageModel;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;

/**
 * Verifies if the extension is annotated with {@link Xml} and if so, enriches the {@link ExtensionDeclarer} with a
 * {@link XmlDslModel}.
 * <p>
 * To get a hold of the {@link Class} on which the {@link Xml} annotation is expected to be, the {@link DescribingContext} will be
 * queried for such a model property. If such property is not present, then this enricher will return without any side effects
 *
 * @since 4.0
 */
public final class XmlModelEnricher extends AbstractAnnotatedModelEnricher {

  @Override
  public void enrich(DescribingContext describingContext) {
    Xml xml = extractAnnotation(describingContext.getExtensionDeclarer().getDeclaration(), Xml.class);
    ExtensionDeclarer declarer = describingContext.getExtensionDeclarer();
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    declarer.withXmlDsl(createXmlLanguageModel(xml, extensionDeclaration.getName(), extensionDeclaration.getVersion()));
  }
}
