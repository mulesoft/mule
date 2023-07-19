/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.semantics;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.sdk.api.annotation.semantics.SemanticTerms;

import java.util.Set;

public final class SemanticTermsParserUtils {

  public static void addCustomTerms(WithAnnotations annotated, Set<String> terms) {
    annotated.getValueFromAnnotation(SemanticTerms.class)
        .ifPresent(semanticTermsAnnotationValueFetcher -> semanticTermsAnnotationValueFetcher.getArrayValue(SemanticTerms::value)
            .forEach(term -> {
              if (!isBlank(term)) {
                terms.add(term.trim());
              }
            }));
  }

  public static void addTermIfPresent(Set<String> searchSpace, String searchTerm, String mappedTerm, Set<String> targetSpace) {
    if (searchSpace.contains(searchTerm)) {
      targetSpace.add(mappedTerm);
    }
  }

  private SemanticTermsParserUtils() {}
}
