/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
