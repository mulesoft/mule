/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.ast.internal.serialization.json.JsonArtifactAstSerializerFormat.JSON;
import static org.mule.runtime.config.api.ArtifactContextFactory.createArtifactContextFactory;
import static org.mule.runtime.config.api.dsl.ArtifactDeclarationUtils.toArtifactast;
import static org.mule.runtime.module.artifact.activation.api.ast.ArtifactAstUtils.parseAndBuildAppExtensionModel;

import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ArtifactType;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.config.api.ArtifactContextFactory;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesHierarchyBuilder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.dsl.api.xml.parser.ParsingPropertyResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;

/**
 * {@link AbstractConfigurationBuilder} implementation that delegates to {@link ArtifactAstConfigurationBuilder} using cached
 * {@link AstXmlParser} instances.
 * 
 * @since 4.5
 */
public class ArtifactAstXmlParserConfigurationBuilder extends AbstractConfigurationBuilder
    implements ArtifactContextFactory {

  public static final String SERIALIZE_DESERIALIZE_AST_PROPERTY = SYSTEM_PROPERTY_PREFIX + "test.serializeDeserializeAst";

  private static final LoadingCache<XmlParserFactory, AstXmlParser> parsersCache =
      newBuilder()
          .maximumSize(2)
          .build(XmlParserFactory::createMuleXmlParser);

  private static final Cache<List<String>, ArtifactAst> configAstsCache =
      newBuilder()
          .maximumSize(2)
          .build();

  private final Map<String, String> artifactProperties;
  private final boolean disableXmlValidations;
  private final boolean enableLazyInit;
  private final boolean addToolingObjectsToRegistry;
  private final boolean ignoreCaches;

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
    this.disableXmlValidations = false;
    this.enableLazyInit = enableLazyInit;
    this.addToolingObjectsToRegistry = addToolingObjectsToRegistry;
    this.ignoreCaches = false;

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
    this.disableXmlValidations = disableXmlValidations;
    this.enableLazyInit = enableLazyInit;
    this.addToolingObjectsToRegistry = addToolingObjectsToRegistry;
    this.ignoreCaches = ignoreCaches;

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
    } else if (ignoreCaches) {
      artifactAst = parseArtifactIntoAst(extensions, muleContext, expressionLanguageMetadataService);
    } else {
      artifactAst = configAstsCache.get(asList(configResources),
                                        key -> parseArtifactIntoAst(extensions, muleContext, expressionLanguageMetadataService));
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

  private AstXmlParser getParser(Set<ExtensionModel> extensions, boolean disableValidations) {
    XmlParserFactory xmlParserFactory = new XmlParserFactory(disableValidations, extensions, artifactProperties,
                                                             artifactType,
                                                             parentArtifactContext != null
                                                                 ? parentArtifactContext.getArtifactAst()
                                                                 : emptyArtifact());

    return ignoreCaches
        ? xmlParserFactory.createMuleXmlParser()
        : parsersCache.get(xmlParserFactory);
  }

  protected ArtifactAst parseArtifactIntoAst(Set<ExtensionModel> extensions, MuleContext muleContext,
                                             ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    try {
      ArtifactAst ast = parseAndBuildAppExtensionModel(muleContext.getConfiguration().getId(),
                                                       configResources,
                                                       this::getParser,
                                                       extensions,
                                                       disableXmlValidations,
                                                       muleContext.getExecutionClassLoader(),
                                                       muleContext.getConfiguration(),
                                                       expressionLanguageMetadataService);

      if (getBoolean(SERIALIZE_DESERIALIZE_AST_PROPERTY)) {
        return serializeAndDeserialize(ast);
      } else {
        return ast;
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private ArtifactAst serializeAndDeserialize(ArtifactAst artifactAst) throws IOException {
    ArtifactAstSerializer jsonArtifactAstSerializer = new ArtifactAstSerializerProvider().getSerializer(JSON, "1.0");
    InputStream inputStream = jsonArtifactAstSerializer.serialize(artifactAst);
    ArtifactAstDeserializer defaultArtifactAstDeserializer = new ArtifactAstSerializerProvider().getDeserializer();
    ArtifactAst deserializedArtifactAst = defaultArtifactAstDeserializer
        .deserialize(inputStream, name -> artifactAst.dependencies().stream()
            .filter(x -> x.getName().equals(name))
            .findFirst()
            .orElse(null));

    return deserializedArtifactAst;
  }

  @Override
  public ArtifactContext createArtifactContext() {
    return artifactAstConfigurationBuilder.createArtifactContext();
  }

  private org.mule.runtime.core.api.config.bootstrap.ArtifactType resolveArtifactType() {
    switch (artifactType) {
      case APPLICATION:
        return org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
      case DOMAIN:
        return org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
      case POLICY:
        return org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
      default:
        return null;
    }
  }

  private static final class XmlParserFactory {

    private final boolean disableXmlValidations;
    private final Set<ExtensionModel> extensions;
    private final Map<String, String> artifactProperties;
    private final ArtifactType artifactType;
    private final ArtifactAst parentArtifactAst;

    public XmlParserFactory(boolean disableXmlValidations,
                            Set<ExtensionModel> extensions,
                            Map<String, String> artifactProperties,
                            ArtifactType artifactType,
                            ArtifactAst parentArtifactAst) {
      this.disableXmlValidations = disableXmlValidations;
      this.extensions = extensions;
      this.artifactProperties = artifactProperties;
      this.artifactType = artifactType;
      this.parentArtifactAst = parentArtifactAst;
    }

    public AstXmlParser createMuleXmlParser() {
      Builder builder = AstXmlParser.builder()
          .withPropertyResolver(createConfigurationPropertiesResolver(artifactProperties))
          .withExtensionModels(extensions)
          .withArtifactType(artifactType)
          .withParentArtifact(parentArtifactAst);
      if (disableXmlValidations) {
        builder.withSchemaValidationsDisabled();
      }

      return builder.build();
    }

    private ParsingPropertyResolver createConfigurationPropertiesResolver(Map<String, String> artifactProperties) {
      ConfigurationPropertiesResolver resolver = new ConfigurationPropertiesHierarchyBuilder()
          .withApplicationProperties(artifactProperties)
          .build();

      return propertyKey -> (String) resolver.resolveValue(propertyKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(artifactProperties, disableXmlValidations, extensions, artifactType, parentArtifactAst);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      XmlParserFactory other = (XmlParserFactory) obj;
      return Objects.equals(artifactProperties, other.artifactProperties)
          && disableXmlValidations == other.disableXmlValidations
          && Objects.equals(extensions, other.extensions)
          && Objects.equals(artifactType, other.artifactType)
          && Objects.equals(parentArtifactAst, other.parentArtifactAst);
    }

  }

}
