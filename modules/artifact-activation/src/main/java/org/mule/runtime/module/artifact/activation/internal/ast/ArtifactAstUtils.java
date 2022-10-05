/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.ast;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.module.artifact.activation.api.ast.AstXmlParserSupplier;

import java.io.IOException;
import java.util.Set;

/**
 * Utilities for parsing and handling {@link ArtifactAst} (for internal usage only).
 *
 * @since 4.5.0
 */
public class ArtifactAstUtils {

  /**
   * Parses {@code configResources} for a Mule artifact and returns an {@link ArtifactAst} enriched with an additional
   * {@link ExtensionModel} which models the artifact itself, with all its defined operations, sources, functions, etc.
   * <p>
   * This extra {@link ExtensionModel} is accessible through the {@link ArtifactAst#dependencies()} set its named after the
   * {@code artifactId}.
   *
   * @param configResources         the paths to the artifact's config files
   * @param parserSupplier          the supplier used to obtain the ast parser. It might be invoked several times during the
   *                                parsing
   * @param extensions              the initial set of extensions the artifact depends on.
   * @param disableValidations      whether to disable DSL validation
   * @param artifactClassLoader     the artifact's classloader
   * @param extensionModelsEnricher an enricher capable of providing additional {@link ExtensionModel}s for the artifact.
   * @return an {@link ArtifactAst}
   * @throws ConfigurationException it the artifact couldn't be parsed
   */
  public static ArtifactAst parseArtifactWithExtensionsEnricher(String[] configResources,
                                                                AstXmlParserSupplier parserSupplier,
                                                                Set<ExtensionModel> extensions,
                                                                boolean disableValidations,
                                                                ClassLoader artifactClassLoader,
                                                                ArtifactExtensionModelsEnricher extensionModelsEnricher)
      throws ConfigurationException {

    final ArtifactAst partialAst = doParseArtifactIntoAst(configResources, parserSupplier, extensions, true);

    if (extensionModelsEnricher.isApplicable(partialAst)) {
      Set<ExtensionModel> enrichedExtensionModels =
          extensionModelsEnricher.getEnrichedExtensionModels(partialAst, artifactClassLoader, extensions);
      if (!enrichedExtensionModels.equals(extensions)) {
        return doParseArtifactIntoAst(configResources, parserSupplier, enrichedExtensionModels, disableValidations);
      }
    }

    return disableValidations
        ? partialAst
        : doParseArtifactIntoAst(configResources, parserSupplier, extensions, false);
  }

  private static ArtifactAst doParseArtifactIntoAst(String[] configResources,
                                                    AstXmlParserSupplier parserSupplier,
                                                    Set<ExtensionModel> extensions,
                                                    boolean disableValidations)
      throws ConfigurationException {
    return parserSupplier.getParser(extensions, disableValidations).parse(loadConfigResources(configResources));
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

  private ArtifactAstUtils() {
    // Empty private constructor to avoid instantiation.
  }
}
