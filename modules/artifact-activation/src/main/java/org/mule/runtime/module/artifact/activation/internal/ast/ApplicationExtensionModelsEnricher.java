/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.ast;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.util.boot.ExtensionLoaderUtils.getOptionalLoaderById;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_NAME_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_THIS_LOADER_ID;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ArtifactType;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

/**
 * An {@link ArtifactExtensionModelsEnricher} suitable for the context of applications.
 *
 * @since 4.5.0
 */
public class ApplicationExtensionModelsEnricher implements ArtifactExtensionModelsEnricher {

  private static final Logger LOGGER = getLogger(ArtifactAstUtils.class);

  // TODO W-11796759: This class shouldn't know which are the specific reusable components.
  private static final Set<TypedComponentIdentifier.ComponentType> REUSABLE_COMPONENT_TYPES = singleton(OPERATION_DEF);

  private final String artifactId;
  private final Optional<String> artifactVersion;
  private final ExpressionLanguageMetadataService expressionLanguageMetadataService;

  public ApplicationExtensionModelsEnricher(String artifactId, Optional<String> artifactVersion,
                                            ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    this.artifactId = artifactId;
    this.artifactVersion = artifactVersion;
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;
  }

  @Override
  public boolean applicable(ArtifactAst ast) {
    return ast.topLevelComponentsStream()
        .anyMatch(component -> REUSABLE_COMPONENT_TYPES.contains(component.getComponentType()));
  }

  @Override
  public Set<ExtensionModel> getEnrichedExtensionModels(ArtifactAst ast, ClassLoader classLoader,
                                                        Set<ExtensionModel> extensions) {
    Optional<ExtensionModel> artifactExtensionModel =
        parseApplicationExtensionModel(ast, classLoader, extensions);

    if (artifactExtensionModel.isPresent()) {
      Set<ExtensionModel> enrichedExtensionModels = new HashSet<>(extensions);
      enrichedExtensionModels.add(artifactExtensionModel.get());
      return enrichedExtensionModels;
    }

    return extensions;
  }

  /**
   * Returns an {@link ExtensionModel} representing an application which defines reusable components (operations, sources, etc).
   *
   * @param ast                 the application's AST
   * @param artifactClassLoader the application's classloader
   * @param dependencies        the dependencies in context.
   * @return an optional {@link ExtensionModel}
   */
  public Optional<ExtensionModel> parseApplicationExtensionModel(ArtifactAst ast,
                                                                 ClassLoader artifactClassLoader,
                                                                 Set<ExtensionModel> dependencies) {
    if (!artifactVersion.isPresent()) {
      logModelNotGenerated("No version specified", artifactId, ast.getArtifactType());
      return empty();
    }

    Optional<ExtensionModelLoader> loader = getOptionalLoaderById(this.getClass().getClassLoader(), MULE_SDK_THIS_LOADER_ID);
    if (loader.isPresent()) {
      return of(loader.get()
          .loadExtensionModel(builder(artifactClassLoader, getDefault(dependencies))
              .addParameter(VERSION_PROPERTY_NAME, artifactVersion.get())
              .addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, ast)
              .addParameter(MULE_SDK_EXTENSION_NAME_PROPERTY_NAME, artifactId)
              .build()));
    } else {
      logModelNotGenerated("Mule ExtensionModelLoader not found", artifactId, ast.getArtifactType());
      return empty();
    }
  }

  private void logModelNotGenerated(String reason, String artifactId, ArtifactType artifactType) {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn("ExtensionModel for {} {} not generated: {}", artifactType, artifactId, reason);
    }
  }
}
