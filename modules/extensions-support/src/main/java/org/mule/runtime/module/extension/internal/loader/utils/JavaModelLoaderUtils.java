/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.utils;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.sdk.api.metadata.NullMetadataResolver.NULL_RESOLVER_NAME;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.KeyIdResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputResolverModelParser;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class JavaModelLoaderUtils {

  public static boolean isScope(MethodElement methodElement) {
    return methodElement.getParameters().stream().anyMatch(JavaModelLoaderUtils::isProcessorChain);
  }

  /**
   * @param methodElement an element representing an operation
   * @return whether the operation is a router
   */
  public static boolean isRouter(MethodElement methodElement) {
    return !getRoutes(methodElement).isEmpty();
  }

  /**
   * @param methodElement an element representing an operation
   * @return a list with the method parameters which represent a {@link Route}
   * @since 4.5.0
   */
  public static List<ExtensionParameter> getRoutes(MethodElement methodElement) {
    return methodElement.getParameters().stream()
        .filter(JavaModelLoaderUtils::isRoute)
        .collect(toList());
  }

  /**
   * @param method a method element
   * @return whether the given {@code method} defines a non-blocking operation
   */
  public static boolean isNonBlocking(MethodElement method) {
    return !getCompletionCallbackParameters(method).isEmpty();
  }

  /**
   * @param method an element representing an operation
   * @return a list with the method parameters which represent a {@link CompletionCallback}
   * @since 4.5.0
   */
  public static List<ExtensionParameter> getCompletionCallbackParameters(MethodElement method) {
    return method.getParameters().stream()
        .filter(p -> p.getType().isAssignableTo(CompletionCallback.class) ||
            p.getType().isAssignableTo(org.mule.sdk.api.runtime.process.CompletionCallback.class))
        .collect(toList());
  }

  /**
   * @param parameter
   * @return whether the given {@code parameter} represents a chain
   */
  public static boolean isProcessorChain(ExtensionParameter parameter) {
    return parameter.getType().isAssignableTo(Chain.class)
        || parameter.getType().isAssignableTo(org.mule.sdk.api.runtime.route.Chain.class);
  }

  /**
   * @param type a {@link MetadataType}
   * @return whether the given {@code type} represents an {@link InputStream} or not
   */
  public static boolean isInputStream(MetadataType type) {
    return isAssignableFrom(type, InputStream.class);
  }

  /**
   * @param metadataType a metadata type
   * @param type         a class
   * @return whether the {@code metadataType} is derived from a java class which is assignable from the {@code type}
   */
  private static boolean isAssignableFrom(MetadataType metadataType, Class<?> type) {
    return getType(metadataType).map(clazz -> type.isAssignableFrom(clazz)).orElse(false);
  }

  /**
   * @param parameter a parameter
   * @return whether the given parameter represents a route
   */
  public static boolean isRoute(ExtensionParameter parameter) {
    return parameter.getType().isAssignableTo(Route.class)
        || parameter.getType().isAssignableTo(org.mule.sdk.api.runtime.route.Route.class);
  }

  /**
   * @param sourceElement a source
   * @return whether the given source emits response or not
   * @since 4.5.0
   */
  public static boolean emitsResponse(SourceElement sourceElement) {
    return sourceElement.isAnnotatedWith(EmitsResponse.class)
        || sourceElement.isAnnotatedWith(org.mule.sdk.api.annotation.source.EmitsResponse.class);
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
   * @since 4.5.0
   */
  public static void declareTypeResolversInformationModelProperty(BaseDeclaration baseDeclaration,
                                                                  Optional<OutputResolverModelParser> outputResolverModelParser,
                                                                  Optional<AttributesResolverModelParser> attributesResolverModelParser,
                                                                  List<InputResolverModelParser> inputResolverModelParsers,
                                                                  Optional<KeyIdResolverModelParser> keyIdResolverModelParser,
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
          .map(KeyIdResolverModelParser::isPartialKeyResolver).orElse(false);

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
   * @since 4.5.0
   */
  public static void declareMetadataResolverFactoryModelProperty(BaseDeclaration baseDeclaration,
                                                                 Optional<OutputResolverModelParser> outputResolverModelParser,
                                                                 Optional<AttributesResolverModelParser> attributesResolverModelParser,
                                                                 List<InputResolverModelParser> inputResolverModelParsers,
                                                                 Optional<KeyIdResolverModelParser> keyIdResolverModelParser) {
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

      TypeKeysResolver typeKeysResolver = keyIdResolverModelParser.map(KeyIdResolverModelParser::getKeyResolver)
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
   * @param keyIdResolverModelParser  parser with the key id metadata
   *
   * @return the category name of the resolvers if present, null otherwise
   *
   * @since 4.5.0
   */
  public static String getCategoryName(KeyIdResolverModelParser keyIdResolverModelParser,
                                       List<InputResolverModelParser> inputResolverModelParsers,
                                       OutputResolverModelParser outputResolverModelParser) {
    if (keyIdResolverModelParser != null && keyIdResolverModelParser.hasKeyIdResolver()) {
      return keyIdResolverModelParser.getKeyResolver().getCategoryName();
    } else if (!inputResolverModelParsers.isEmpty()) {
      return inputResolverModelParsers.iterator().next().getInputResolver().getCategoryName();
    } else if (outputResolverModelParser != null && outputResolverModelParser.hasOutputResolver()) {
      return outputResolverModelParser.getOutputResolver().getCategoryName();
    } else {
      return null;
    }
  }
}
