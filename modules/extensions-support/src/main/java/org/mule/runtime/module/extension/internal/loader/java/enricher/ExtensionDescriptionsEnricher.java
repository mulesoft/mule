/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.enricher;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DISABLE_DESCRIPTIONS_ENRICHMENT;
import static org.mule.runtime.module.extension.internal.resources.documentation.ExtensionDescriptionsSerializer.SERIALIZER;

import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.util.DeclarationWalker;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.resources.documentation.XmlExtensionDocumentation;
import org.mule.runtime.module.extension.internal.resources.documentation.XmlExtensionElementDocumentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Declarer that adds descriptions to a {@link ExtensionDeclaration} by using the SDK generated
 * <strong>extensions-descriptions.xml</strong> file which persists the descriptions for each element (Configurations, Providers,
 * Operations, Parameters, ...) in the extension.
 * <p>
 * This is necessary because such documentation is not available once the extension source code is compiled.
 * <p>
 * If the <strong>extensions-descriptions.xml</strong> does not exist, this enricher won't declare any descriptions.
 *
 * @since 4.0
 */
public final class ExtensionDescriptionsEnricher implements DeclarationEnricher {

  /**
   * {@inheritDoc}
   */
  @Override
  public void enrich(ExtensionLoadingContext loadingContext) {
    if (isDisableDscriptionsEnrichment(loadingContext)) {
      return;
    }

    String name = loadingContext.getExtensionDeclarer().getDeclaration().getName();
    ClassLoader classLoader = loadingContext.getExtensionClassLoader();
    try (InputStream resource = classLoader.getResourceAsStream("META-INF/" + SERIALIZER.getFileName(name))) {
      if (resource != null) {
        XmlExtensionDocumentation documenter = withContextClassLoader(
                                                                      ExtensionDescriptionsEnricher.class
                                                                          .getClassLoader(),
                                                                      () -> SERIALIZER
                                                                          .deserialize(resource));
        document(loadingContext.getExtensionDeclarer().getDeclaration(), documenter);
      }
    } catch (IOException e) {
      throw new RuntimeException("Cannot get descriptions persisted in the extensions-descriptions.xml file", e);
    }
  }

  public boolean isDisableDscriptionsEnrichment(ExtensionLoadingContext loadingContext) {
    return loadingContext.getParameter(DISABLE_DESCRIPTIONS_ENRICHMENT)
        .map(v -> v instanceof Boolean ? (Boolean) v : false)
        .orElse(false);
  }

  /**
   * Fills all the descriptions in the provided {@link ExtensionDeclaration} based on the
   * <strong>extensions-descriptions.xml</strong> file.
   *
   * @param declaration   the declaration to describe.
   * @param documentation the extension documentation with its corresponding description.
   */
  private void document(ExtensionDeclaration declaration, XmlExtensionDocumentation documentation) {
    declaration.setDescription(documentation.getExtension().getDescription());
    new DeclarationWalker() {

      @Override
      protected void onConfiguration(ConfigurationDeclaration declaration) {
        document(declaration, documentation.getConfigs());
      }

      @Override
      protected void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration) {
        document(declaration, documentation.getOperations());
      }

      @Override
      protected void onConnectionProvider(ConnectedDeclaration owner, ConnectionProviderDeclaration declaration) {
        document(declaration, documentation.getConnections());
      }

      @Override
      protected void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration) {
        document(declaration, documentation.getSources());
      }

      private void document(ParameterizedDeclaration<?> declaration, List<XmlExtensionElementDocumentation> elements) {
        elements.stream().filter(e -> e.getName().equals(declaration.getName())).findAny()
            .ifPresent(e -> {
              declaration.setDescription(e.getDescription());
              declaration.getAllParameters()
                  .forEach(param -> e.getParameters().stream().filter(p -> p.getName().equals(param.getName())).findAny()
                      .ifPresent(p -> param.setDescription(p.getDescription())));
            });
      }
    }.walk(declaration);
  }

}
