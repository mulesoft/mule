/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;
import static org.mule.runtime.extension.api.util.XmlModelUtils.createXmlLanguageModel;
import static org.mule.sdk.api.metadata.NullMetadataResolver.NULL_RESOLVER_NAME;

import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSemanticTermsDeclaration;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.metadata.internal.DefaultMetadataResolverFactory;
import org.mule.runtime.metadata.internal.NullMetadataResolverFactory;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.MetadataKeyModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SemanticTermsParser;
import org.mule.runtime.module.extension.internal.loader.parser.XmlDslConfiguration;
import org.mule.sdk.api.annotation.dsl.xml.Xml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

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
   * Declares the model property {@link TypeResolversInformationModelProperty} on the given {@link BaseDeclaration declaration}
   *
   * @param baseDeclaration               the declaration
   * @param outputResolverModelParser     parser with the output metadata
   * @param attributesResolverModelParser parser with the attributes metadata
   * @param inputResolverModelParsers     parser with the input metadata
   * @param keyIdResolverModelParser      parser with the key id metadata
   * @param requiresConnection            indicates if the resolution of metadata requieres a connecion and configuration
   *
   * @since 4.6.0
   */
  public static void declareTypeResolversInformationModelProperty(BaseDeclaration baseDeclaration,
                                                                  Optional<OutputResolverModelParser> outputResolverModelParser,
                                                                  Optional<AttributesResolverModelParser> attributesResolverModelParser,
                                                                  List<InputResolverModelParser> inputResolverModelParsers,
                                                                  Optional<MetadataKeyModelParser> keyIdResolverModelParser,
                                                                  boolean requiresConnection) {

    if ((outputResolverModelParser.isPresent() && outputResolverModelParser.get().hasOutputResolver()) ||
        !inputResolverModelParsers.isEmpty()) {

      Map<String, String> inputResolversByParam = inputResolverModelParsers.stream()
          .collect(toImmutableMap(InputResolverModelParser::getParameterName, r -> r.getInputResolver().getResolverName()));

      String outputResolver = outputResolverModelParser
          .map(outputResolverParser -> outputResolverParser.getOutputResolver().getResolverName())
          .orElse(NULL_RESOLVER_NAME);

      String attributesResolver = attributesResolverModelParser
          .map(attributesResolverParser -> attributesResolverParser.getAttributesResolver().getResolverName())
          .orElse(NULL_RESOLVER_NAME);

      String keysResolver = keyIdResolverModelParser
          .map(keyResolverParser -> keyResolverParser.getKeyResolver().getResolverName())
          .orElse(NULL_RESOLVER_NAME);

      boolean isPartialKeyResolver = keyIdResolverModelParser
          .map(MetadataKeyModelParser::isPartialKeyResolver).orElse(false);

      String categoryName = getCategoryName(keyIdResolverModelParser.orElse(null), inputResolverModelParsers,
                                            outputResolverModelParser.orElse(null));

      TypeResolversInformationModelProperty typeResolversInformationModelProperty = new TypeResolversInformationModelProperty(
                                                                                                                              categoryName,
                                                                                                                              inputResolversByParam,
                                                                                                                              outputResolver,
                                                                                                                              attributesResolver,
                                                                                                                              keysResolver,
                                                                                                                              requiresConnection,
                                                                                                                              requiresConnection,
                                                                                                                              isPartialKeyResolver);

      baseDeclaration.addModelProperty(typeResolversInformationModelProperty);
    }
  }

  /**
   * Declares the model property {@link MetadataResolverFactoryModelProperty} on the given {@link BaseDeclaration declaration}
   *
   * @param baseDeclaration               the declaration
   * @param outputResolverModelParser     parser with the output metadata
   * @param attributesResolverModelParser parser with the attributes metadata
   * @param inputResolverModelParsers     parser with the input metadata
   * @param keyIdResolverModelParser      parser with the key id metadata
   *
   * @since 4.6.0
   */
  public static void declareMetadataResolverFactoryModelProperty(BaseDeclaration baseDeclaration,
                                                                 Optional<OutputResolverModelParser> outputResolverModelParser,
                                                                 Optional<AttributesResolverModelParser> attributesResolverModelParser,
                                                                 List<InputResolverModelParser> inputResolverModelParsers,
                                                                 Optional<MetadataKeyModelParser> keyIdResolverModelParser) {
    MetadataResolverFactory metadataResolverFactory;
    if ((outputResolverModelParser.isPresent() && outputResolverModelParser.get().hasOutputResolver()) ||
        !inputResolverModelParsers.isEmpty()) {

      NullMetadataResolver nullMetadataResolver = new NullMetadataResolver();

      OutputTypeResolver<?> outputTypeResolver = outputResolverModelParser.map(OutputResolverModelParser::getOutputResolver)
          .orElse((OutputTypeResolver) nullMetadataResolver);
      Supplier<OutputTypeResolver<?>> outputTypeResolverSupplier = () -> outputTypeResolver;

      AttributesTypeResolver<?> attributesTypeResolver =
          attributesResolverModelParser.map(AttributesResolverModelParser::getAttributesResolver)
              .orElse((AttributesTypeResolver) nullMetadataResolver);
      Supplier<AttributesTypeResolver<?>> attributesTypeResolverSupplier = () -> attributesTypeResolver;

      TypeKeysResolver typeKeysResolver = keyIdResolverModelParser.map(MetadataKeyModelParser::getKeyResolver)
          .orElse(nullMetadataResolver);
      Supplier<TypeKeysResolver> typeKeysResolverSupplier = () -> typeKeysResolver;

      Map<String, Supplier<? extends InputTypeResolver>> inputTypeResolvers = new HashMap<>();
      inputResolverModelParsers.forEach(parser -> inputTypeResolvers.put(parser.getParameterName(), parser::getInputResolver));

      metadataResolverFactory = new DefaultMetadataResolverFactory(typeKeysResolverSupplier, inputTypeResolvers,
                                                                   outputTypeResolverSupplier,
                                                                   attributesTypeResolverSupplier);
    } else {
      metadataResolverFactory = new NullMetadataResolverFactory();
    }

    baseDeclaration.addModelProperty(new MetadataResolverFactoryModelProperty(() -> metadataResolverFactory));
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
   * @since 4.6.0
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
}
