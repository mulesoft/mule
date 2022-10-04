/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.ast;

import static org.mule.runtime.module.artifact.activation.internal.ast.ArtifactAstUtils.parseArtifactWithExtensionsEnricher;

import static java.util.Optional.empty;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ArtifactType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.module.artifact.activation.internal.ast.ApplicationExtensionModelEnricher;

import java.util.Optional;
import java.util.Set;

/**
 * Utilities for parsing and handling {@link ArtifactAst}
 *
 * @since 4.5.0
 */
public final class ArtifactAstUtils {

  /**
   * Parses {@code configResources} for a Mule application and returns an {@link ArtifactAst} enriched with an additional
   * {@link ExtensionModel} which models the app itself, with all its defined operations, sources, functions, etc.
   * <p>
   * This extra {@link ExtensionModel} is accessible through the {@link ArtifactAst#dependencies()} set its named after the
   * {@code muleContext.getConfiguration.getId()} return value
   *
   * @param configResources    the paths to the application's config files
   * @param parserSupplier     the supplier used to obtain the ast parser. It might be invoked several times during the parsing
   * @param extensions         the initial set of extensions the app depends on.
   * @param artifactType       the artifact type
   * @param disableValidations whether to disable DSL validation
   * @param muleContext        the app's {@link MuleContext}
   * @return an {@link ArtifactAst}
   * @throws ConfigurationException it the artifact couldn't be parsed
   */
  public static ArtifactAst parseAndBuildAppExtensionModel(String[] configResources,
                                                           AstXmlParserSupplier parserSupplier,
                                                           Set<ExtensionModel> extensions,
                                                           ArtifactType artifactType,
                                                           boolean disableValidations,
                                                           MuleContext muleContext)
      throws ConfigurationException {
    return parseArtifact(configResources,
                         parserSupplier,
                         extensions,
                         disableValidations,
                         muleContext.getExecutionClassLoader().getParent(),
                         muleContext.getConfiguration().getId(),
                         muleContext.getConfiguration().getArtifactCoordinates().map(ArtifactCoordinates::getVersion));
  }

  /**
   * Parses {@code configResources} for a Mule artifact and returns an {@link ArtifactAst} enriched with an additional
   * {@link ExtensionModel} which models the artifact itself, with all its defined operations, sources, functions, etc.
   * <p>
   * This extra {@link ExtensionModel} is accessible through the {@link ArtifactAst#dependencies()} set its named after the
   * {@code artifactId}.
   *
   * @param configResources     the paths to the artifact's config files
   * @param parserSupplier      the supplier used to obtain the ast parser. It might be invoked several times during the parsing
   * @param extensions          the initial set of extensions the artifact depends on.
   * @param disableValidations  whether to disable DSL validation
   * @param artifactClassLoader the artifact's classloader
   * @param artifactId          the artifact's ID.
   * @param artifactVersion     the artifact's version.
   * @return an {@link ArtifactAst}
   * @throws ConfigurationException it the artifact couldn't be parsed
   */
  public static ArtifactAst parseArtifact(String[] configResources,
                                          AstXmlParserSupplier parserSupplier,
                                          Set<ExtensionModel> extensions,
                                          boolean disableValidations,
                                          ClassLoader artifactClassLoader,
                                          String artifactId,
                                          Optional<String> artifactVersion)
      throws ConfigurationException {

    return parseArtifactWithExtensionsEnricher(configResources,
                                               parserSupplier,
                                               extensions,
                                               disableValidations,
                                               artifactClassLoader,
                                               new ApplicationExtensionModelEnricher(artifactId, artifactVersion));
  }

  /**
   * If the {@code ast} represents an application which defines reusable components (operations, sources, etc), it returns an
   * {@link ExtensionModel} which represents it.
   *
   * @param ast                 the artifact's AST
   * @param artifactClassLoader the artifact's classloader
   * @param muleContext         the artifact's context
   * @return an optional {@link ExtensionModel}
   */
  public static Optional<ExtensionModel> parseArtifactExtensionModel(ArtifactAst ast,
                                                                     ClassLoader artifactClassLoader,
                                                                     MuleContext muleContext) {
    return parseArtifactExtensionModel(ast,
                                       artifactClassLoader,
                                       muleContext.getConfiguration().getId(),
                                       muleContext.getConfiguration().getArtifactCoordinates()
                                           .map(ArtifactCoordinates::getVersion),
                                       muleContext.getExtensionManager().getExtensions());
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
   * @return an optional {@link ExtensionModel}
   */
  public static Optional<ExtensionModel> parseArtifactExtensionModel(ArtifactAst ast,
                                                                     ClassLoader artifactClassLoader,
                                                                     String artifactId,
                                                                     Optional<String> artifactVersion,
                                                                     Set<ExtensionModel> dependencies) {
    ApplicationExtensionModelEnricher enricher = new ApplicationExtensionModelEnricher(artifactId, artifactVersion);
    if (!enricher.applicable(ast)) {
      return empty();
    }

    return enricher.parseApplicationExtensionModel(ast, artifactClassLoader, dependencies);
  }

  private ArtifactAstUtils() {}
}
