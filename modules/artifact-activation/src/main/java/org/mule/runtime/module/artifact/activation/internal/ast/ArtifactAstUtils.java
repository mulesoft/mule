/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.ast;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_MULE_SDK_PROPERTY;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;

import static java.lang.Boolean.getBoolean;
import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.module.artifact.activation.api.ast.AstXmlParserSupplier;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.w3c.dom.Document;

/**
 * Utilities for parsing and handling {@link ArtifactAst} (for internal usage only).
 *
 * @since 4.5.0
 */
public class ArtifactAstUtils {

  private static final boolean IS_MULE_SDK_ENABLED = getBoolean(ENABLE_MULE_SDK_PROPERTY);

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
  public static ArtifactAst parseArtifact(Either<String[], Map<String, Document>> configs,
                                          AstXmlParserSupplier parserSupplier,
                                          Set<ExtensionModel> extensions,
                                          boolean disableValidations,
                                          ClassLoader artifactClassLoader,
                                          MuleSdkExtensionModelLoadingMediator extensionModelMediator)
      throws ConfigurationException {
    if (!IS_MULE_SDK_ENABLED) {
      return doParseArtifactIntoAst(configs, parserSupplier, extensions, disableValidations, artifactClassLoader);
    }

    final ArtifactAst partialAst = doParseArtifactIntoAst(configs, parserSupplier, extensions, true, artifactClassLoader);

    Optional<ExtensionModel> extensionModel =
        extensionModelMediator.loadExtensionModel(partialAst, artifactClassLoader.getParent(), extensions);
    if (extensionModel.isPresent()) {
      Set<ExtensionModel> enrichedExtensionModels = new HashSet<>(extensions);
      enrichedExtensionModels.add(extensionModel.get());
      return doParseArtifactIntoAst(configs, parserSupplier, enrichedExtensionModels, disableValidations,
                                    artifactClassLoader);
    }

    return disableValidations || configs.isRight()
        ? partialAst
        : doParseArtifactIntoAst(configs, parserSupplier, extensions, false, artifactClassLoader);
  }

  private static ArtifactAst doParseArtifactIntoAst(Either<String[], Map<String, Document>> configs,
                                                    AstXmlParserSupplier parserSupplier,
                                                    Set<ExtensionModel> extensions,
                                                    boolean disableValidations,
                                                    ClassLoader artifactClassLoader)
      throws ConfigurationException {
    if (configs.isLeft()) {
      return parserSupplier.getParser(extensions, disableValidations)
          .parse(loadConfigResources(configs.getLeft(), artifactClassLoader));
    } else {
      return configs.mapRight(appXmlConfigDocuments -> {
        final AstXmlParser parser = parserSupplier.getParser(extensions, true);

        ClassLoader currentClassLoader = currentThread().getContextClassLoader();
        setContextClassLoader(currentThread(), currentClassLoader, artifactClassLoader);
        try {
          return parser.parseDocument(appXmlConfigDocuments.entrySet()
              .stream()
              .map(e -> new Pair<>(e.getKey(), e.getValue()))
              .collect(toList()));
        } finally {
          setContextClassLoader(currentThread(), artifactClassLoader, currentClassLoader);
        }
      })
          .getRight();
    }
  }

  private static ConfigResource[] loadConfigResources(String[] configs, ClassLoader artifactClassLoader)
      throws ConfigurationException {
    ClassLoader currentClassLoader = currentThread().getContextClassLoader();
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
