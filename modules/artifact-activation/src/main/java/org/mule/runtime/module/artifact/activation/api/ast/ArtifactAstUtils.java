/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.ast;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.core.api.util.boot.ExtensionLoaderUtils.getOptionalLoaderById;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_NAME_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_LOADER_ID;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.of;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ArtifactType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;

/**
 * Utilities for parsing and handling {@link ArtifactAst}
 *
 * @since 4.5.0
 */
public final class ArtifactAstUtils {

  private static final Logger LOGGER = getLogger(ArtifactAstUtils.class);

  private static final Set<ComponentType> APPLICATION_COMPONENT_TYPES = unmodifiableSet(of(OPERATION_DEF));

  /**
   * Parses {@code configResources} for a Mule application and returns an {@link ArtifactAst} enriched with an additional
   * {@link ExtensionModel} which models the app itself, with all its defined operations, sources, functions, etc.
   * <p>
   * This extra {@link ExtensionModel} is accessible through the {@link ArtifactAst#dependencies()} set its named after the
   * {@code muleContext.getConfiguration.getId()} return value
   *
   * @param configResources                   the paths to the application's config files
   * @param parserSupplier                    the supplier used to obtain the ast parser. It might be invoked several times during
   *                                          the parsing
   * @param extensions                        the initial set of extensions the app depends on.
   * @param artifactType                      the artifact type
   * @param disableValidations                whether to disable DSL validation
   * @param muleContext                       the app's {@link MuleContext}
   * @param expressionLanguageMetadataService the {@link ExpressionLanguageMetadataService} used to resolve types.
   * @return an {@link ArtifactAst}
   * @throws ConfigurationException it the app couldn't be parsed
   */
  public static ArtifactAst parseAndBuildAppExtensionModel(String[] configResources,
                                                           AstXmlParserSupplier parserSupplier,
                                                           Set<ExtensionModel> extensions,
                                                           ArtifactType artifactType,
                                                           boolean disableValidations,
                                                           MuleContext muleContext,
                                                           ExpressionLanguageMetadataService expressionLanguageMetadataService)
      throws ConfigurationException {
    return parseArtifact(configResources,
                         ArtifactAstUtils::containsReusableAssets,
                         parserSupplier,
                         extensions,
                         artifactType,
                         disableValidations,
                         muleContext.getExecutionClassLoader().getParent(),
                         muleContext.getConfiguration().getId(),
                         muleContext.getConfiguration().getArtifactCoordinates().map(ArtifactCoordinates::getVersion),
                         expressionLanguageMetadataService);
  }

  /**
   * Parses {@code configResources} for a Mule artifact and returns an {@link ArtifactAst} enriched with an additional
   * {@link ExtensionModel} which models the artifact itself, with all its defined operations, sources, functions, etc.
   * <p>
   * This extra {@link ExtensionModel} is accessible through the {@link ArtifactAst#dependencies()} set its named after the
   * {@code artifactId}.
   *
   * @param configResources            the paths to the artifact's config files
   * @param hasReusableAssetsPredicate a predicate to test if the artifact contains reusable assets.
   * @param parserSupplier             the supplier used to obtain the ast parser. It might be invoked several times during the
   *                                   parsing
   * @param extensions                 the initial set of extensions the artifact depends on.
   * @param artifactType               the artifact type
   * @param disableValidations         whether to disable DSL validation
   * @param artifactClassLoader        the artifact's classloader
   * @param artifactId                 the artifact's ID.
   * @param artifactVersion            the artifact's version.
   * @param expressionLanguageMetadataService the {@link ExpressionLanguageMetadataService} used to resolve types.
   * @return an {@link ArtifactAst}
   */
  public static ArtifactAst parseArtifact(String[] configResources,
                                          Predicate<ArtifactAst> hasReusableAssetsPredicate,
                                          AstXmlParserSupplier parserSupplier,
                                          Set<ExtensionModel> extensions,
                                          ArtifactType artifactType,
                                          boolean disableValidations,
                                          ClassLoader artifactClassLoader,
                                          String artifactId,
                                          Optional<String> artifactVersion,
                                          ExpressionLanguageMetadataService expressionLanguageMetadataService) {

    final ArtifactAst partialAst = doParseArtifactIntoAst(configResources, parserSupplier, extensions, true);

    if (artifactType.equals(APPLICATION) && hasReusableAssetsPredicate.test(partialAst)) {
      ExtensionModel artifactExtensionModel =
          parseArtifactExtensionModel(partialAst, artifactClassLoader, artifactId, artifactVersion, extensions,
                                      expressionLanguageMetadataService).orElse(null);
      if (artifactExtensionModel != null) {
        Set<ExtensionModel> enrichedExtensionModels = new HashSet<>(extensions);
        enrichedExtensionModels.add(artifactExtensionModel);
        return doParseArtifactIntoAst(configResources, parserSupplier, enrichedExtensionModels, disableValidations);
      }
    }

    return disableValidations
        ? partialAst
        : doParseArtifactIntoAst(configResources, parserSupplier, extensions, false);
  }

  /**
   * If the {@code ast} represents an application which defines reusable components (operations, sources, etc), it returns an
   * {@link ExtensionModel} which represents it.
   *
   * @param ast                               the application's AST
   * @param artifactClassLoader               the application's classloader
   * @param muleContext                       the application's context
   * @param expressionLanguageMetadataService the {@link ExpressionLanguageMetadataService} used to resolve types.
   * @return an optional {@link ExtensionModel}
   */
  public static Optional<ExtensionModel> parseArtifactExtensionModel(ArtifactAst ast,
                                                                     ClassLoader artifactClassLoader,
                                                                     MuleContext muleContext,
                                                                     ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    if (!containsReusableAssets(ast)) {
      return empty();
    }

    return parseArtifactExtensionModel(ast,
                                       artifactClassLoader,
                                       muleContext.getConfiguration().getId(),
                                       muleContext.getConfiguration().getArtifactCoordinates()
                                           .map(ArtifactCoordinates::getVersion),
                                       muleContext.getExtensionManager().getExtensions(),
                                       expressionLanguageMetadataService);
  }

  /**
   * If the {@code ast} represents an artifact which defines reusable components (operations, sources, etc), it returns an
   * {@link ExtensionModel} which represents it.
   *
   * @param ast                 the artifact's AST
   * @param artifactClassLoader the artifact's classloader
   * @param artifactId          the artifact's ID.
   * @param artifactVersion     the artifact's version.
   * @param dependencies        the dependencies in context.
   * @param expressionLanguageMetadataService the {@link ExpressionLanguageMetadataService} used to resolve types.
   * @return an optional {@link ExtensionModel}
   */
  public static Optional<ExtensionModel> parseArtifactExtensionModel(ArtifactAst ast,
                                                                     ClassLoader artifactClassLoader,
                                                                     String artifactId,
                                                                     Optional<String> artifactVersion,
                                                                     Set<ExtensionModel> dependencies,
                                                                     ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    if (!artifactVersion.isPresent()) {
      logModelNotGenerated("No version specified", artifactId);
      return empty();
    }
    Optional<ExtensionModelLoader> loader = getOptionalLoaderById(ArtifactAstUtils.class.getClassLoader(), MULE_SDK_LOADER_ID);
    if (loader.isPresent()) {
      return of(loader.get()
          .loadExtensionModel(builder(artifactClassLoader, getDefault(dependencies))
              .addParameter(VERSION_PROPERTY_NAME, artifactVersion.get())
              .addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, ast)
              .addParameter(MULE_SDK_EXTENSION_NAME_PROPERTY_NAME, artifactId)
              .build()));
    } else {
      logModelNotGenerated("Mule ExtensionModelLoader not found", artifactId);
      return empty();
    }
  }

  private static ArtifactAst doParseArtifactIntoAst(String[] configResources,
                                                    AstXmlParserSupplier parserSupplier,
                                                    Set<ExtensionModel> extensions,
                                                    boolean disableValidations) {
    try {
      return parserSupplier.getParser(extensions, disableValidations).parse(loadConfigResources(configResources));
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private static boolean containsReusableAssets(ArtifactAst artifactAst) {
    return artifactAst.topLevelComponentsStream()
        .anyMatch(component -> APPLICATION_COMPONENT_TYPES.contains(component.getComponentType()));
  }

  private static ConfigResource[] loadConfigResources(String[] configs) throws ConfigurationException {
    try {
      ConfigResource[] artifactConfigResources = new ConfigResource[configs.length];
      for (int i = 0; i < configs.length; i++) {
        artifactConfigResources[i] = new ConfigResource(configs[i]);
      }
      return artifactConfigResources;
    } catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }

  private static void logModelNotGenerated(String reason, String artifactId) {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn("ExtensionModel for {} not generated: {}", artifactId, reason);
    }
  }

  private ArtifactAstUtils() {}
}
