/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.ast;

import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.module.artifact.activation.internal.ast.ArtifactAstUtils.parseArtifact;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ArtifactType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.module.artifact.activation.internal.ast.MuleSdkApplicationExtensionModelLoadingMediator;
import org.mule.runtime.module.artifact.activation.internal.ast.MuleSdkExtensionModelLoadingMediator;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.w3c.dom.Document;

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
   * @deprecated Use
   *             {@link #parseAndBuildAppExtensionModel(String[], AstXmlParserSupplier, Set, boolean, MuleContext, ExpressionLanguageMetadataService)}
   *             instead;
   */
  @Deprecated
  public static ArtifactAst parseAndBuildAppExtensionModel(String[] configResources,
                                                           AstXmlParserSupplier parserSupplier,
                                                           Set<ExtensionModel> extensions,
                                                           ArtifactType artifactType,
                                                           boolean disableValidations,
                                                           MuleContext muleContext,
                                                           ExpressionLanguageMetadataService expressionLanguageMetadataService)
      throws ConfigurationException {
    return parseArtifact(left(configResources),
                         parserSupplier,
                         extensions,
                         disableValidations,
                         muleContext.getExecutionClassLoader(),
                         getExtensionModelLoadingMediator(muleContext, expressionLanguageMetadataService));
  }

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
   * 
   * @since 4.8
   */
  public static ArtifactAst parseAndBuildAppExtensionModel(String[] configResources,
                                                           AstXmlParserSupplier parserSupplier,
                                                           Set<ExtensionModel> extensions,
                                                           boolean disableValidations,
                                                           MuleContext muleContext,
                                                           ExpressionLanguageMetadataService expressionLanguageMetadataService)
      throws ConfigurationException {
    return parseArtifact(left(configResources),
                         parserSupplier,
                         extensions,
                         disableValidations,
                         muleContext.getExecutionClassLoader(),
                         getExtensionModelLoadingMediator(muleContext, expressionLanguageMetadataService));
  }

  /**
   * Processes {@code appXmlConfigDocuments} for a Mule application and returns an {@link ArtifactAst} enriched with an additional
   * {@link ExtensionModel} which models the app itself, with all its defined operations, sources, functions, etc.
   * <p>
   * This extra {@link ExtensionModel} is accessible through the {@link ArtifactAst#dependencies()} set its named after the
   * {@code muleContext.getConfiguration.getId()} return value
   *
   * @param appXmlConfigDocuments             the parsed XML DOMs for the config files
   * @param parserSupplier                    the supplier used to obtain the ast parser. It might be invoked several times during
   *                                          the parsing
   * @param extensions                        the initial set of extensions the app depends on.
   * @param artifactType                      the artifact type
   * @param disableValidations                whether to disable DSL validation
   * @param muleContext                       the app's {@link MuleContext}
   * @param expressionLanguageMetadataService the {@link ExpressionLanguageMetadataService} used to resolve types.
   * @return an {@link ArtifactAst}
   * @throws ConfigurationException it the app couldn't be parsed
   * 
   * @since 4.8
   */
  public static ArtifactAst parseAndBuildAppExtensionModel(Map<String, Document> appXmlConfigDocuments,
                                                           AstXmlParserSupplier parserSupplier,
                                                           Set<ExtensionModel> extensions,
                                                           boolean disableValidations,
                                                           MuleContext muleContext,
                                                           ExpressionLanguageMetadataService expressionLanguageMetadataService)
      throws ConfigurationException {
    return parseArtifact(right(appXmlConfigDocuments),
                         parserSupplier,
                         extensions,
                         disableValidations,
                         muleContext.getExecutionClassLoader(),
                         getExtensionModelLoadingMediator(muleContext, expressionLanguageMetadataService));
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
                                                                     ExpressionLanguageMetadataService expressionLanguageMetadataService)
      throws ConfigurationException {
    return getExtensionModelLoadingMediator(muleContext, expressionLanguageMetadataService)
        .loadExtensionModel(ast, artifactClassLoader, muleContext.getExtensionManager().getExtensions());
  }

  private static MuleSdkExtensionModelLoadingMediator getExtensionModelLoadingMediator(MuleContext muleContext,
                                                                                       ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    String artifactId = muleContext.getConfiguration().getId();
    Optional<ArtifactCoordinates> artifactCoordinates = muleContext.getConfiguration().getArtifactCoordinates();
    return new MuleSdkApplicationExtensionModelLoadingMediator(expressionLanguageMetadataService, artifactId,
                                                               artifactCoordinates);
  }

  private ArtifactAstUtils() {}
}
