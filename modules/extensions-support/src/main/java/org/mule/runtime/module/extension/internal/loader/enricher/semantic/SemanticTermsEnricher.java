/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher.semantic;

import static org.mule.runtime.extension.api.connectivity.ConnectivityVocabulary.NTLM_PROXY_CONFIGURATION;
import static org.mule.runtime.extension.api.connectivity.ConnectivityVocabulary.NTLM_PROXY_CONFIGURATION_PARAMETER;
import static org.mule.runtime.extension.api.connectivity.ConnectivityVocabulary.PROXY_CONFIGURATION_PARAMETER;
import static org.mule.runtime.extension.api.connectivity.ConnectivityVocabulary.PROXY_CONFIGURATION_TYPE;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.STRUCTURE;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getSemanticTerms;
import static org.mule.runtime.extension.internal.semantic.SemanticTermsHelper.getTermsFromAnnotations;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSemanticTermsDeclaration;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.enricher.AbstractAnnotatedDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.MethodWrapper;

import java.util.Set;

public class SemanticTermsEnricher extends AbstractAnnotatedDeclarationEnricher {

  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
        extractType(declaration).ifPresent(type -> addSemanticTerms(declaration, type));
      }

      @Override
      protected void onOperation(OperationDeclaration declaration) {
        extractImplementingMethod(declaration)
            .map(method -> new MethodWrapper(method, typeLoader))
            .ifPresent(method -> addSemanticTerms(declaration, method));
      }

      @Override
      protected void onSource(SourceDeclaration declaration) {
        extractType(declaration).ifPresent(type -> addSemanticTerms(declaration, type));
      }

      @Override
      protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration parameter) {
        extractDeclaredParameter(parameter).ifPresent(e -> addSemanticTerms(parameter, e));
        Set<String> typeTerms = getSemanticTerms(parameter.getType());
        addTermIfPresent(typeTerms, parameter, PROXY_CONFIGURATION_TYPE, PROXY_CONFIGURATION_PARAMETER);
        addTermIfPresent(typeTerms, parameter, NTLM_PROXY_CONFIGURATION, NTLM_PROXY_CONFIGURATION_PARAMETER);
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private void addTermIfPresent(Set<String> terms, WithSemanticTermsDeclaration declaration, String term, String mappedTerm) {
    if (terms.contains(term)) {
      declaration.addSemanticTerm(mappedTerm);
    }
  }

  private void addSemanticTerms(WithSemanticTermsDeclaration declaration, WithAnnotations base) {
    getTermsFromAnnotations(base::isAnnotatedWith).forEach(declaration::addSemanticTerm);
  }
}
