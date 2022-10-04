/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.ast;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;

import java.util.Set;

/**
 * Allows for enriching the {@link ExtensionModel}s available in the context of the {@link ArtifactAst} being parsed.
 *
 * @since 4.5.0
 */
public interface ArtifactExtensionModelsEnricher {

  /**
   * @param ast the artifact's AST
   * @return whether the enricher should be applied to the given {@link ArtifactAst}.
   */
  boolean applicable(ArtifactAst ast);

  /**
   * @param ast         the artifact's AST
   * @param classLoader the artifact's classloader
   * @param extensions  the initial set of extensions the artifact depends on.
   * @return a potentially enriched {@link Set} of {@link ExtensionModel}s.
   */
  Set<ExtensionModel> getEnrichedExtensionModels(ArtifactAst ast, ClassLoader classLoader, Set<ExtensionModel> extensions);
}
