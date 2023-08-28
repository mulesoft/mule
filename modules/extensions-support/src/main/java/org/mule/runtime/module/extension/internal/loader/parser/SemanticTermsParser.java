/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import java.util.Set;

/**
 * General contract for a model parser capable of reading semantic terms
 *
 * @since 4.5.0
 */
public interface SemanticTermsParser {

  /**
   * @return a {@link Set} with the model's semantic terms
   */
  Set<String> getSemanticTerms();
}
