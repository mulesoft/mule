/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher.semantic;

import static org.mule.runtime.connectivity.api.platform.schema.ConnectivityVocabulary.NTLM_PROXY_CONFIGURATION;
import static org.mule.runtime.connectivity.api.platform.schema.ConnectivityVocabulary.NTLM_PROXY_CONFIGURATION_PARAMETER;
import static org.mule.runtime.connectivity.api.platform.schema.ConnectivityVocabulary.PROXY_CONFIGURATION_PARAMETER;
import static org.mule.runtime.connectivity.api.platform.schema.ConnectivityVocabulary.PROXY_CONFIGURATION_TYPE;
import static org.mule.runtime.connectivity.api.platform.schema.ConnectivityVocabulary.SCALAR_SECRET;
import static org.mule.runtime.connectivity.api.platform.schema.ConnectivityVocabulary.SECRET;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getSemanticTerms;
import static org.mule.runtime.connectivity.internal.platform.schema.SemanticTermsHelper.getConnectionTermsFromAnnotations;
import static org.mule.runtime.connectivity.internal.platform.schema.SemanticTermsHelper.getParameterTermsFromAnnotations;
import static org.mule.runtime.connectivity.internal.platform.schema.SemanticTermsHelper.getAllTermsFromAnnotations;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.visitor.BasicTypeMetadataVisitor;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSemanticTermsDeclaration;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.enricher.AbstractAnnotatedDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.MethodWrapper;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An enricher which adds semantic terms based on annotations
 *
 * @since 4.4.0ExtensionDocumentationResourceGenerator.java
 */
public class SemanticTermsEnricher extends AbstractAnnotatedDeclarationEnricher {

  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
        extractType(declaration).ifPresent(type -> addSemanticTerms(getConnectionTermsFromAnnotations(type::isAnnotatedWith), declaration));
      }

      @Override
      protected void onOperation(OperationDeclaration declaration) {
        extractImplementingMethod(declaration)
            .map(method -> new MethodWrapper(method, typeLoader))
            .ifPresent(method -> addSemanticTerms(getAllTermsFromAnnotations(method::isAnnotatedWith), declaration));
      }

      @Override
      protected void onSource(SourceDeclaration declaration) {
        extractType(declaration).ifPresent(type -> addSemanticTerms(getAllTermsFromAnnotations(type::isAnnotatedWith), declaration));
      }

      @Override
      protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration parameter) {
        extractDeclaredParameter(parameter).ifPresent(e -> addSemanticTerms(getParameterTermsFromAnnotations(e::isAnnotatedWith), parameter));
        Set<String> typeTerms = new LinkedHashSet<>(getSemanticTerms(parameter.getType()));

        addTermIfPresent(typeTerms, parameter, PROXY_CONFIGURATION_TYPE, PROXY_CONFIGURATION_PARAMETER);
        addTermIfPresent(typeTerms, parameter, NTLM_PROXY_CONFIGURATION, NTLM_PROXY_CONFIGURATION_PARAMETER);
        if (typeTerms.contains(SECRET)) {
          parameter.getType().accept(new BasicTypeMetadataVisitor() {

            @Override
            protected void visitBasicType(MetadataType metadataType) {
              typeTerms.remove(SECRET);
              typeTerms.add(SCALAR_SECRET);
            }
          });
        }
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private void addTermIfPresent(Set<String> terms, WithSemanticTermsDeclaration declaration, String term, String mappedTerm) {
    if (terms.contains(term)) {
      declaration.addSemanticTerm(mappedTerm);
    }
  }



  private void addSemanticTerms(Set<String> terms, WithSemanticTermsDeclaration declaration) {
    terms.forEach(declaration::addSemanticTerm);
  }
}
