/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.ast.internal.serialization.json.JsonArtifactAstSerializerFormat.JSON;
import static org.mule.runtime.module.artifact.activation.api.ast.ArtifactAstUtils.parseAndBuildAppExtensionModel;

import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.asList;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ArtifactType;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesHierarchyBuilder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.dsl.api.xml.parser.ParsingPropertyResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;

public class CachingAstXmlParser {

  public static final String SERIALIZE_DESERIALIZE_AST_PROPERTY = SYSTEM_PROPERTY_PREFIX + "test.serializeDeserializeAst";

  private static final LoadingCache<XmlParserFactory, AstXmlParser> parsersCache =
      newBuilder()
          .maximumSize(2)
          .build(XmlParserFactory::createMuleXmlParser);

  private static final Cache<List<String>, ArtifactAst> configAstsCache =
      newBuilder()
          .maximumSize(2)
          .build();

  private final boolean disableXmlValidations;
  private final boolean ignoreCaches;

  private final Map<String, String> artifactProperties;
  private final ArtifactType artifactType;
  private final ArtifactAst parent;

  public CachingAstXmlParser(boolean disableXmlValidations, boolean ignoreCaches,
                             Map<String, String> artifactProperties, ArtifactType artifactType, ArtifactAst parent) {
    this.disableXmlValidations = disableXmlValidations;
    this.ignoreCaches = ignoreCaches;
    this.artifactProperties = artifactProperties;
    this.artifactType = artifactType;
    this.parent = parent;
  }

  public ArtifactAst parse(String artifactName,
                           Set<ExtensionModel> extensions,
                           ClassLoader artifactClassLoader,
                           Optional<ArtifactCoordinates> artifactCoordinates,
                           ExpressionLanguageMetadataService expressionLanguageMetadataService,
                           String[] configResources) {
    if (ignoreCaches) {
      return parseArtifactIntoAst(artifactName, extensions, artifactClassLoader, artifactCoordinates,
                                  expressionLanguageMetadataService, configResources);
    } else {
      return configAstsCache.get(asList(configResources),
                                 key -> parseArtifactIntoAst(artifactName, extensions, artifactClassLoader, artifactCoordinates,
                                                             expressionLanguageMetadataService,
                                                             configResources));
    }
  }

  private ArtifactAst parseArtifactIntoAst(String artifactName,
                                           Set<ExtensionModel> extensions,
                                           ClassLoader artifactClassLoader,
                                           Optional<ArtifactCoordinates> artifactCoordinates,
                                           ExpressionLanguageMetadataService expressionLanguageMetadataService,
                                           String[] configResources) {
    try {
      ArtifactAst ast = parseAndBuildAppExtensionModel(artifactName,
                                                       configResources,
                                                       this::getParser,
                                                       extensions,
                                                       disableXmlValidations,
                                                       artifactClassLoader,
                                                       artifactCoordinates,
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

  private AstXmlParser getParser(Set<ExtensionModel> extensions, boolean disableValidations) {
    XmlParserFactory xmlParserFactory = new XmlParserFactory(disableValidations, extensions,
                                                             artifactProperties,
                                                             artifactType,
                                                             parent);

    return ignoreCaches
        ? xmlParserFactory.createMuleXmlParser()
        : parsersCache.get(xmlParserFactory);
  }

  private ArtifactAst serializeAndDeserialize(ArtifactAst artifactAst) throws IOException {
    ArtifactAstSerializer jsonArtifactAstSerializer = new ArtifactAstSerializerProvider().getSerializer(JSON, "1.0");
    InputStream inputStream = jsonArtifactAstSerializer.serialize(artifactAst);
    ArtifactAstDeserializer defaultArtifactAstDeserializer = new ArtifactAstSerializerProvider().getDeserializer();
    ArtifactAst deserializedArtifactAst = defaultArtifactAstDeserializer
        .deserialize(inputStream, name -> artifactAst.dependencies().stream()
            .filter(x -> x.getName().equals(name))
            .findFirst()
            .orElse(null), artifactAst.getParent().orElse(null));

    return deserializedArtifactAst;
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
