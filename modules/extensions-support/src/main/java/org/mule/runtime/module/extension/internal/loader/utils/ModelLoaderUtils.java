/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.util.XmlModelUtils.createXmlLanguageModel;
import static org.mule.runtime.module.extension.api.metadata.ComponentMetadataConfigurer.configureNullMetadata;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSemanticTermsDeclaration;
import org.mule.runtime.api.metadata.resolving.NamedTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.metadata.ComponentMetadataConfigurer;
import org.mule.runtime.module.extension.internal.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SemanticTermsParser;
import org.mule.runtime.module.extension.internal.loader.parser.XmlDslConfiguration;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.MetadataKeyModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.OutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.RoutesChainInputTypesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.ScopeChainInputTypeResolverModelParser;
import org.mule.sdk.api.annotation.dsl.xml.Xml;

import java.util.List;
import java.util.Optional;

/**
 * Utility class for {@link ModelLoaderDelegate model loaders}
 *
 * @since 4.0
 */
public final class ModelLoaderUtils {

  private ModelLoaderUtils() {}

  /**
   * Adds all the semantic terms in the {@code parser} into the given {@code declaration}
   *
   * @param declaration a declaration
   * @param parser      a parser
   * @since 4.5.0
   */
  public static void addSemanticTerms(WithSemanticTermsDeclaration declaration, SemanticTermsParser parser) {
    declaration.getSemanticTerms().addAll(parser.getSemanticTerms());
  }

  /**
   * Utility method to obtain a default {@link XmlDslModel} of a given {@link XmlDslConfiguration}
   *
   * @param extensionName                 the name of the extension
   * @param version                       version of the extension
   * @param xmlDslAnnotationConfiguration configuration of {@link org.mule.runtime.extension.api.annotation.dsl.xml.Xml} of
   *                                      {@link Xml}
   * @return the {@link XmlDslModel}
   * @since 4.5.0
   */
  public static XmlDslModel getXmlDslModel(String extensionName,
                                           String version,
                                           Optional<XmlDslConfiguration> xmlDslAnnotationConfiguration) {
    Optional<String> prefix = empty();
    Optional<String> namespace = empty();

    if (xmlDslAnnotationConfiguration.isPresent()) {
      prefix = of(xmlDslAnnotationConfiguration.get().getPrefix());
      namespace = of(xmlDslAnnotationConfiguration.get().getNamespace());
    }

    return createXmlLanguageModel(prefix, namespace, extensionName, version);
  }

  /**
   * Utility method to obtain a default {@link XmlDslModel} of a given {@link XmlDslConfiguration}
   *
   * @param extensionElement              the extension element
   * @param version                       version of the extension
   * @param xmlDslAnnotationConfiguration configuration of {@link org.mule.runtime.extension.api.annotation.dsl.xml.Xml} of
   *                                      {@link Xml}
   * @return the {@link XmlDslModel}
   * @since 4.5.0
   */
  public static XmlDslModel getXmlDslModel(ExtensionElement extensionElement,
                                           String version,
                                           Optional<XmlDslConfiguration> xmlDslAnnotationConfiguration) {
    return getXmlDslModel(extensionElement.getName(), version, xmlDslAnnotationConfiguration);
  }

  /**
   * Declares the model property {@link MetadataResolverFactoryModelProperty} on the given {@link BaseDeclaration declaration}
   *
   * @param declaration                   the declaration
   * @param outputResolverModelParser     parser with the output metadata
   * @param attributesResolverModelParser parser with the attributes metadata
   * @param inputResolverModelParsers     parser with the input metadata
   * @param keyIdResolverModelParser      parser with the key id metadata
   *
   * @since 4.5.0
   */
  public static void declareMetadataResolverFactoryModelProperty(ParameterizedDeclaration declaration,
                                                                 Optional<OutputResolverModelParser> outputResolverModelParser,
                                                                 Optional<AttributesResolverModelParser> attributesResolverModelParser,
                                                                 List<InputResolverModelParser> inputResolverModelParsers,
                                                                 Optional<MetadataKeyModelParser> keyIdResolverModelParser) {
    declareMetadataResolverFactoryModelProperty(declaration,
                                                outputResolverModelParser,
                                                attributesResolverModelParser,
                                                inputResolverModelParsers,
                                                keyIdResolverModelParser,
                                                empty(),
                                                empty());
  }

  public static void declareMetadataResolverFactoryModelProperty(ParameterizedDeclaration declaration,
                                                                 Optional<OutputResolverModelParser> outputResolverModelParser,
                                                                 Optional<AttributesResolverModelParser> attributesResolverModelParser,
                                                                 List<InputResolverModelParser> inputResolverModelParsers,
                                                                 Optional<MetadataKeyModelParser> keyIdResolverModelParser,
                                                                 Optional<ScopeChainInputTypeResolverModelParser> scopeChainInputResolverParser,
                                                                 Optional<RoutesChainInputTypesResolverModelParser> routesChainInputTypesResolverParser) {
    if (outputResolverModelParser.map(p -> p.hasOutputResolver()).orElse(false) || !inputResolverModelParsers.isEmpty()) {
      final ComponentMetadataConfigurer configurer = new ComponentMetadataConfigurer();
      outputResolverModelParser.ifPresent(p -> configurer.setOutputTypeResolver(p.getOutputResolver()));
      attributesResolverModelParser.ifPresent(p -> configurer.setAttributesTypeResolver(p.getAttributesResolver()));
      keyIdResolverModelParser.ifPresent(p -> configurer.setKeysResolver(p.getKeyResolver(), p.getParameterName(),
                                                                         p.getMetadataType(), p.isPartialKeyResolver()));

      inputResolverModelParsers
          .forEach(parser -> configurer.addInputResolver(parser.getParameterName(), parser.getInputResolver()));
      scopeChainInputResolverParser.ifPresent(p -> configurer.setChainInputTypeResolver(p.getChainInputTypeResolver()));
      routesChainInputTypesResolverParser
          .ifPresent(p -> configurer.addRoutesChainInputResolvers(p.getRoutesChainInputResolvers()));

      configurer.configure(declaration);
    } else {
      configureNullMetadata(declaration);
    }
  }

  /**
   * Given the parser of the resolvers of output, input and key id metadata return the category name of the resolvers.
   *
   * @param outputResolverModelParser parser with the output metadata
   * @param inputResolverModelParsers parser with the input metadata
   * @param metadataKeyModelParser    parser with the key id metadata
   *
   * @return the category name of the resolvers if present, null otherwise
   *
   * @since 4.5.0
   */
  public static String getCategoryName(MetadataKeyModelParser metadataKeyModelParser,
                                       List<InputResolverModelParser> inputResolverModelParsers,
                                       OutputResolverModelParser outputResolverModelParser) {
    if (metadataKeyModelParser != null && metadataKeyModelParser.hasKeyIdResolver()) {
      return metadataKeyModelParser.getKeyResolver().getCategoryName();
    } else if (!inputResolverModelParsers.isEmpty()) {
      return inputResolverModelParsers.iterator().next().getInputResolver().getCategoryName();
    } else if (outputResolverModelParser != null && outputResolverModelParser.hasOutputResolver()) {
      return outputResolverModelParser.getOutputResolver().getCategoryName();
    } else {
      return null;
    }
  }

  public static String getCategoryName(TypeKeysResolver typeKeysResolver,
                                       String firstSeenInputResolverCategory,
                                       OutputTypeResolver outputTypeResolver) {
    if (isNotNull(typeKeysResolver)) {
      return typeKeysResolver.getCategoryName();
    } else if (!isBlank(firstSeenInputResolverCategory)) {
      return firstSeenInputResolverCategory;
    } else if (isNotNull(outputTypeResolver)) {
      return outputTypeResolver.getCategoryName();
    } else {
      return null;
    }
  }

  private static boolean isNotNull(Object resolver) {
    if (resolver == null) {
      return false;
    }
    if (resolver instanceof NamedTypeResolver) {
      return !(resolver instanceof NullMetadataResolver);
    } else if (resolver instanceof org.mule.sdk.api.metadata.resolving.NamedTypeResolver) {
      return !(resolver instanceof org.mule.sdk.api.metadata.NullMetadataResolver);
    }

    return true;
  }
}
