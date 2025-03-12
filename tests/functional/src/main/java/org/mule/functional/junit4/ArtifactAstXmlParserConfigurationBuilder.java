/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.config.api.ArtifactContextFactory.createArtifactContextFactory;
import static org.mule.runtime.config.api.dsl.ArtifactDeclarationUtils.toArtifactast;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ArtifactType;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.config.api.ArtifactContextFactory;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;

import java.util.Map;
import java.util.Set;

/**
 * {@link AbstractConfigurationBuilder} implementation that delegates to {@link ArtifactAstConfigurationBuilder} using cached
 * {@link AstXmlParser} instances.
 * 
 * @since 4.5
 */
public class ArtifactAstXmlParserConfigurationBuilder extends AbstractConfigurationBuilder
    implements ArtifactContextFactory {

  public static final String SERIALIZE_DESERIALIZE_AST_PROPERTY = CachingAstXmlParser.SERIALIZE_DESERIALIZE_AST_PROPERTY;

  static {
    System.setProperty(CACHE_COMPONENT_BUILDING_DEFINITION_REGISTRY_PROPERTY, "true");
  }

  private final Map<String, String> artifactProperties;
  private final boolean enableLazyInit;
  private final boolean addToolingObjectsToRegistry;

  private final CachingAstXmlParser cachingAstXmlParser;
  private final ExpressionLanguageMetadataService expressionLanguageMetadataService;

  private ArtifactDeclaration artifactDeclaration;
  private String[] configResources;

  private ArtifactType artifactType = APPLICATION;
  private ArtifactContext parentArtifactContext;

  private ArtifactContextFactory artifactAstConfigurationBuilder;

  public ArtifactAstXmlParserConfigurationBuilder(Map<String, String> artifactProperties,
                                                  boolean enableLazyInit,
                                                  boolean addToolingObjectsToRegistry,
                                                  ArtifactDeclaration artifactDeclaration,
                                                  ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    this.artifactProperties = artifactProperties;
    this.enableLazyInit = enableLazyInit;
    this.addToolingObjectsToRegistry = addToolingObjectsToRegistry;

    this.cachingAstXmlParser = new CachingAstXmlParser(false, false,
                                                       artifactProperties,
                                                       artifactType,
                                                       parentArtifactContext != null
                                                           ? parentArtifactContext.getArtifactAst()
                                                           : emptyArtifact());
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;

    this.artifactDeclaration = requireNonNull(artifactDeclaration);
  }

  public ArtifactAstXmlParserConfigurationBuilder(Map<String, String> artifactProperties,
                                                  boolean disableXmlValidations,
                                                  boolean enableLazyInit,
                                                  boolean addToolingObjectsToRegistry,
                                                  boolean ignoreCaches,
                                                  String[] configResources,
                                                  ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    this.artifactProperties = artifactProperties;
    this.enableLazyInit = enableLazyInit;
    this.addToolingObjectsToRegistry = addToolingObjectsToRegistry;

    this.cachingAstXmlParser = new CachingAstXmlParser(disableXmlValidations, ignoreCaches,
                                                       artifactProperties,
                                                       artifactType,
                                                       parentArtifactContext != null
                                                           ? parentArtifactContext.getArtifactAst()
                                                           : emptyArtifact());
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;

    this.configResources = requireNonNull(configResources);
  }

  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  public void setParentArtifactContext(ArtifactContext parentArtifactContext) {
    this.parentArtifactContext = parentArtifactContext;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    Set<ExtensionModel> extensions = muleContext.getExtensionManager().getExtensions();

    final ArtifactAst artifactAst;
    if (artifactDeclaration != null) {
      artifactAst = toArtifactast(artifactDeclaration, extensions);
    } else if (configResources.length == 0) {
      artifactAst = emptyArtifact();
    } else {
      artifactAst = cachingAstXmlParser.parse(muleContext.getConfiguration().getId(),
                                              extensions,
                                              muleContext.getExecutionClassLoader(),
                                              muleContext.getConfiguration().getArtifactCoordinates(),
                                              expressionLanguageMetadataService,
                                              configResources);
    }

    artifactAstConfigurationBuilder = createArtifactContextFactory(artifactAst,
                                                                   artifactProperties,
                                                                   resolveArtifactType(),
                                                                   enableLazyInit,
                                                                   addToolingObjectsToRegistry,
                                                                   serviceConfigurators,
                                                                   ofNullable(parentArtifactContext));
    artifactAstConfigurationBuilder.configure(muleContext);
  }

  @Override
  public ArtifactContext createArtifactContext() {
    return artifactAstConfigurationBuilder.createArtifactContext();
  }

  private org.mule.runtime.core.api.config.bootstrap.ArtifactType resolveArtifactType() {
    return switch (artifactType) {
      case APPLICATION -> org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
      case DOMAIN -> org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
      case POLICY -> org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
      default -> null;
    };
  }

}
