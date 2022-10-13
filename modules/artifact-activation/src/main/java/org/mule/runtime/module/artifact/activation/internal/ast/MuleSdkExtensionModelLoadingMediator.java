/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.ast;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.config.ConfigurationException;

import java.util.Optional;
import java.util.Set;

/**
 * Allows for extracting an {@link ExtensionModel} which represents the {@link ArtifactAst} being parsed.
 *
 * @since 4.5.0
 */
public interface MuleSdkExtensionModelLoadingMediator {

  /**
   * @param ast         the artifact's AST
   * @param classLoader the artifact's classloader
   * @param extensions  the initial set of extensions the artifact depends on.
   * @return an {@link ExtensionModel} that represents the {@code ast}.
   * @throws ConfigurationException if the artifact couldn't be parsed.
   */
  Optional<ExtensionModel> loadExtensionModel(ArtifactAst ast, ClassLoader classLoader, Set<ExtensionModel> extensions)
      throws ConfigurationException;
}
