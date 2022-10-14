/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.ast;

import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;

import static java.lang.Thread.currentThread;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.module.artifact.activation.api.ast.AstXmlParserSupplier;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
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
   * This extra {@link ExtensionModel} is accessible through the {@link ArtifactAst#dependencies()}.
   *
   * @param configResources        the paths to the artifact's config files
   * @param parserSupplier         the supplier used to obtain the ast parser. It might be invoked several times during the
   *                               parsing
   * @param extensions             the initial set of extensions the artifact depends on.
   * @param disableValidations     whether to disable DSL validation
   * @param artifactClassLoader    the artifact's classloader
   * @param extensionModelMediator a mediator capable of providing the {@link ExtensionModel} which models the artifact itself.
   * @return an {@link ArtifactAst}
   * @throws ConfigurationException it the artifact couldn't be parsed
   */
  public static ArtifactAst parseArtifact(String[] configResources,
                                          AstXmlParserSupplier parserSupplier,
                                          Set<ExtensionModel> extensions,
                                          boolean disableValidations,
                                          ClassLoader artifactClassLoader,
                                          MuleSdkExtensionModelLoadingMediator extensionModelMediator)
      throws ConfigurationException {

    final ArtifactAst partialAst = doParseArtifactIntoAst(configResources, parserSupplier, extensions, true, artifactClassLoader);

    Optional<ExtensionModel> extensionModel =
        extensionModelMediator.loadExtensionModel(partialAst, artifactClassLoader.getParent(), extensions);
    if (extensionModel.isPresent()) {
      Set<ExtensionModel> enrichedExtensionModels = new HashSet<>(extensions);
      enrichedExtensionModels.add(extensionModel.get());
      return doParseArtifactIntoAst(configResources, parserSupplier, enrichedExtensionModels, disableValidations,
                                    artifactClassLoader);
    }

    return disableValidations
        ? partialAst
        : doParseArtifactIntoAst(configResources, parserSupplier, extensions, false, artifactClassLoader);
  }

  private static ArtifactAst doParseArtifactIntoAst(String[] configResources,
                                                    AstXmlParserSupplier parserSupplier,
                                                    Set<ExtensionModel> extensions,
                                                    boolean disableValidations,
                                                    ClassLoader artifactClassLoader)
      throws ConfigurationException {
    return parserSupplier.getParser(extensions, disableValidations)
        .parse(loadConfigResources(configResources, artifactClassLoader));
  }

  private static ConfigResource[] loadConfigResources(String[] configs, ClassLoader artifactClassLoader)
      throws ConfigurationException {
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    setContextClassLoader(currentThread(), currentClassLoader, artifactClassLoader);
    try {
      ConfigResource[] artifactConfigResources = new ConfigResource[configs.length];
      for (int i = 0; i < configs.length; i++) {
        artifactConfigResources[i] = new ConfigResource(configs[i]);
      }
      return artifactConfigResources;
    } catch (IOException e) {
      throw new ConfigurationException(e);
    } finally {
      setContextClassLoader(currentThread(), artifactClassLoader, currentClassLoader);
    }
  }

  private ArtifactAstUtils() {
    // Empty private constructor to avoid instantiation.
  }
}
