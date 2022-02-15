/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.ast.internal.serialization.ArtifactAstSerializerFactory.JSON;
import static org.mule.runtime.config.api.dsl.ArtifactDeclarationUtils.toArtifactast;
import static org.mule.runtime.config.internal.ConfigurationPropertiesResolverFactory.createConfigurationPropertiesResolver;
import static org.mule.runtime.core.api.util.boot.ExtensionLoaderUtils.getOptionalLoaderById;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_NAME_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_LOADER_ID;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_TYPE_LOADER_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.module.extension.internal.loader.AbstractExtensionModelLoader.VERSION;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ArtifactType;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.config.api.ArtifactContextFactory;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.config.internal.ComponentBuildingDefinitionRegistryFactoryAware;
import org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.artifact.ArtifactCoordinates;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.type.catalog.ApplicationTypeLoader;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    implements ComponentBuildingDefinitionRegistryFactoryAware, ArtifactContextFactory {

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
  private final boolean ignoreCaches;

  private ArtifactDeclaration artifactDeclaration;
  private String[] configResources;

  private ArtifactType artifactType = APPLICATION;
  private ArtifactContext parentArtifactContext;

  private ComponentBuildingDefinitionRegistryFactory componentBuildingDefinitionRegistryFactory;
  private ArtifactAstConfigurationBuilder artifactAstConfigurationBuilder;

  public ArtifactAstXmlParserConfigurationBuilder(Map<String, String> artifactProperties,
                                                  boolean enableLazyInit,
                                                  ArtifactDeclaration artifactDeclaration) {
    this.artifactProperties = artifactProperties;
    this.disableXmlValidations = false;
    this.enableLazyInit = enableLazyInit;
    this.ignoreCaches = false;

    this.artifactDeclaration = requireNonNull(artifactDeclaration);
  }

  public ArtifactAstXmlParserConfigurationBuilder(Map<String, String> artifactProperties,
                                                  boolean disableXmlValidations,
                                                  boolean enableLazyInit,
                                                  boolean ignoreCaches,
                                                  String[] configResources) {
    this.artifactProperties = artifactProperties;
    this.disableXmlValidations = disableXmlValidations;
    this.enableLazyInit = enableLazyInit;
    this.ignoreCaches = ignoreCaches;

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
      artifactAst = parseArtifactIntoAst(extensions, muleContext);
    } else {
      artifactAst = configAstsCache.get(asList(configResources), key -> parseArtifactIntoAst(extensions, muleContext));
    }

    artifactAstConfigurationBuilder =
        new ArtifactAstConfigurationBuilder(artifactAst, artifactProperties, resolveArtifactType(), enableLazyInit);
    this.serviceConfigurators.forEach(artifactAstConfigurationBuilder::addServiceConfigurator);
    artifactAstConfigurationBuilder.setComponentBuildingDefinitionRegistryFactory(componentBuildingDefinitionRegistryFactory);
    if (parentArtifactContext != null) {
      artifactAstConfigurationBuilder.setParentContext(parentArtifactContext.getMuleContext(),
                                                       parentArtifactContext.getArtifactAst());
    }
    artifactAstConfigurationBuilder.configure(muleContext);
  }

  private ArtifactAst doParseArtifactIntoAst(Set<ExtensionModel> extensions, boolean disableValidations) {
    XmlParserFactory xmlParserFactory = new XmlParserFactory(disableValidations, extensions, artifactProperties,
                                                             artifactType,
                                                             parentArtifactContext != null
                                                                 ? parentArtifactContext.getArtifactAst()
                                                                 : emptyArtifact());
    AstXmlParser astXmlParser;
    if (ignoreCaches) {
      astXmlParser = xmlParserFactory.createMuleXmlParser();
    } else {
      astXmlParser = parsersCache.get(xmlParserFactory);
    }

    try {
      return astXmlParser.parse(loadConfigResources(configResources));
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  protected ArtifactAst parseArtifactIntoAst(Set<ExtensionModel> extensions, MuleContext muleContext) {
    final ArtifactAst partialAst = doParseArtifactIntoAst(extensions, true);

    final ArtifactAst effectiveAst = parseApplicationExtensionModel(partialAst, muleContext)
        .map(extensionModel -> {
          Set<ExtensionModel> enrichedExtensionModels = new HashSet<>(extensions);
          enrichedExtensionModels.add(extensionModel);
          return doParseArtifactIntoAst(enrichedExtensionModels, true); // TODO: use disableXmlValidations field
        }).orElseGet(() -> doParseArtifactIntoAst(extensions, disableXmlValidations));


    if (getBoolean(SERIALIZE_DESERIALIZE_AST_PROPERTY)) {
      try {
        return seralizeAndDeserialize(effectiveAst);
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    } else {
      return effectiveAst;
    }
  }

  private Optional<ExtensionModel> parseApplicationExtensionModel(ArtifactAst ast, MuleContext muleContext) {
    if (!artifactType.equals(APPLICATION)) {
      return empty();
    }

    Optional<ArtifactCoordinates> artifactCoordinates = muleContext.getConfiguration().getArtifactCoordinates();

    if (!artifactCoordinates.isPresent()) {
      logModelNotGenerated("No version specified on muleContext");
      return empty();
    }

    Optional<ExtensionModelLoader> loader = getOptionalLoaderById(getClass().getClassLoader(), MULE_SDK_LOADER_ID);
    if (loader.isPresent()) {
      final ExtensionManager extensionManager = muleContext.getExtensionManager();
      ExtensionModel appExtensionModel = loader.get()
          .loadExtensionModel(builder(muleContext.getExecutionClassLoader().getParent(),
                                      getDefault(extensionManager.getExtensions()))
                                          .addParameter(VERSION, artifactCoordinates.get().getVersion())
                                          .addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, ast)
                                          .addParameter(MULE_SDK_EXTENSION_NAME_PROPERTY_NAME,
                                                        muleContext.getConfiguration().getId())
                                          .addParameter(MULE_SDK_TYPE_LOADER_PROPERTY_NAME, new ApplicationTypeLoader())
                                          .build());

      return of(appExtensionModel);
    } else {
      logModelNotGenerated("Mule ExtensionModelLoader not found");
      return empty();
    }
  }

  private void logModelNotGenerated(String reason) {
    // if (LOGGER.isWarnEnabled()) {
    // LOGGER.warn(reason + ". ExtensionModel for app {} not generated", muleContext.getConfiguration().getId());
    // }
  }

  private ArtifactAst seralizeAndDeserialize(ArtifactAst artifactAst) throws IOException {
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

  private ConfigResource[] loadConfigResources(String[] configs) throws ConfigurationException {
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

  @Override
  public void setComponentBuildingDefinitionRegistryFactory(ComponentBuildingDefinitionRegistryFactory componentBuildingDefinitionRegistryFactory) {
    this.componentBuildingDefinitionRegistryFactory = componentBuildingDefinitionRegistryFactory;
  }

}
