/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.INITIALIZE;
import static org.mule.runtime.extension.api.util.XmlModelUtils.createXmlLanguageModel;

import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;

import java.util.Optional;

/**
 * Verifies if the extension is annotated with {@link Xml} and if so, enriches the {@link ExtensionDeclarer} with a
 * {@link XmlDslModel}.
 * <p>
 * To get a hold of the {@link Class} on which the {@link Xml} annotation is expected to be, the {@link ExtensionLoadingContext}
 * will be queried for such a model property. If such property is not present, then this enricher will return without any side
 * effects
 *
 * @since 4.0
 */
public final class JavaXmlDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return INITIALIZE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    XmlInformation xmlInformation = getXmlInformation(extensionLoadingContext);
    ExtensionDeclarer declarer = extensionLoadingContext.getExtensionDeclarer();
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    declarer.withXmlDsl(getXmlLanguageModel(xmlInformation, extensionDeclaration));
  }

  private XmlDslModel getXmlLanguageModel(XmlInformation xmlInformation, ExtensionDeclaration extensionDeclaration) {
    final Optional<String> extensionNamespace = xmlInformation != null ? ofNullable(xmlInformation.getPrefix()) : empty();
    final Optional<String> extensionNamespaceLocation =
        xmlInformation != null ? ofNullable(xmlInformation.getNamespace()) : empty();
    return createXmlLanguageModel(extensionNamespace, extensionNamespaceLocation, extensionDeclaration.getName(),
                                  extensionDeclaration.getVersion());
  }

  private XmlInformation getXmlInformation(ExtensionLoadingContext extensionLoadingContext) {
    Xml legacyXmlAnnotation = extractAnnotation(extensionLoadingContext.getExtensionDeclarer().getDeclaration(), Xml.class);
    org.mule.sdk.api.annotation.dsl.xml.Xml sdkXmlAnnotation =
        extractAnnotation(extensionLoadingContext.getExtensionDeclarer().getDeclaration(),
                          org.mule.sdk.api.annotation.dsl.xml.Xml.class);

    if (legacyXmlAnnotation != null && sdkXmlAnnotation != null) {
      throw new IllegalModelDefinitionException(format("Annotations %s and %s are both present at the same time on the extension",
                                                       Xml.class.getName(),
                                                       org.mule.sdk.api.annotation.dsl.xml.Xml.class.getName()));
    } else if (legacyXmlAnnotation != null) {
      return new XmlInformation(legacyXmlAnnotation);
    } else if (sdkXmlAnnotation != null) {
      return new XmlInformation(sdkXmlAnnotation);
    } else {
      return null;
    }
  }

  private static class XmlInformation {

    String prefix;
    String namespace;

    public XmlInformation(Xml xml) {
      this.prefix = xml.prefix();
      this.namespace = xml.namespace();
    }

    public XmlInformation(org.mule.sdk.api.annotation.dsl.xml.Xml xml) {
      this.prefix = xml.prefix();
      this.namespace = xml.namespace();
    }

    public String getPrefix() {
      return prefix;
    }

    public String getNamespace() {
      return namespace;
    }
  }
}
