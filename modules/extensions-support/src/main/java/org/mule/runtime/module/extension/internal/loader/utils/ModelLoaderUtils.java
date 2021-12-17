/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import org.mule.runtime.api.meta.model.declaration.fluent.WithSemanticTermsDeclaration;
import org.mule.runtime.module.extension.internal.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.parser.SemanticTermsParser;

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
}
