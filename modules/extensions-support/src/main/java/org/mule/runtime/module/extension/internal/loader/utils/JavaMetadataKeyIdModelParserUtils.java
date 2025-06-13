/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataTypeResolverUtils.isStaticResolver;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.extension.api.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.extension.api.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParserDecorator;
import org.mule.runtime.module.extension.internal.loader.parser.java.HasExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaOperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaSourceModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.metadata.JavaMetadataKeyModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.InputResolverModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.MetadataKeyModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.OutputResolverModelParser;
import org.mule.sdk.api.annotation.metadata.MetadataKeyId;
import org.mule.sdk.api.annotation.metadata.MetadataKeyPart;
import org.mule.sdk.api.annotation.metadata.MetadataScope;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

/**
 * Helper class for introspecting metadata keys
 *
 * @since 4.5
 */
public class JavaMetadataKeyIdModelParserUtils {

  private static final Logger LOGGER = getLogger(JavaMetadataKeyIdModelParserUtils.class);

  public static Optional<MetadataKeyModelParser> parseKeyIdResolverModelParser(ExtensionParameter extensionParameter,
                                                                               String categoryName,
                                                                               String groupName) {
    String parameterName = !isBlank(groupName) ? groupName : extensionParameter.getName();
    MetadataType metadataType = extensionParameter.getType().asMetadataType();
    return mapReduceSingleAnnotation(extensionParameter, "parameter", extensionParameter.getName(),
                                     org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId.class,
                                     MetadataKeyId.class,
                                     valueFetcher -> {
                                       Type type = valueFetcher
                                           .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId::value);
                                       return type.getDeclaringClass()
                                           .map(value -> keyIdResolverFromType(parameterName, categoryName, metadataType, value))
                                           .orElse(null);
                                     },
                                     valueFetcher -> {
                                       Type type = valueFetcher
                                           .getClassValue(MetadataKeyId::value);
                                       return type.getDeclaringClass()
                                           .map(value -> keyIdResolverFromType(parameterName, categoryName, metadataType, value))
                                           .orElse(null);
                                     });
  }

  public static Optional<MetadataKeyModelParser> parseKeyIdResolverModelParser(Type extensionType,
                                                                               WithAnnotations annotatedType, String elementType,
                                                                               String elementName, String extensionName) {
    Optional<MetadataKeyModelParser> keyIdResolverModelParser =
        parseKeyIdResolverModelParser(annotatedType, elementType, elementName);

    if (!keyIdResolverModelParser.isPresent()) {
      keyIdResolverModelParser = parseKeyIdResolverModelParser(extensionType, "extension", extensionName);
    }

    return keyIdResolverModelParser;
  }

  public static Optional<MetadataKeyModelParser> parseKeyIdResolverModelParser(WithAnnotations annotatedType, String elementType,
                                                                               String elementName) {
    return mapReduceSingleAnnotation(annotatedType, elementType, elementName,
                                     org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                     MetadataScope.class,
                                     valueFetcher -> {
                                       Type type = valueFetcher
                                           .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::keysResolver);
                                       return type.getDeclaringClass()
                                           .map(JavaMetadataKeyIdModelParserUtils::keyIdResolverFromTypeOnSources).orElse(null);
                                     },
                                     valueFetcher -> {
                                       Type type = valueFetcher
                                           .getClassValue(MetadataScope::keysResolver);
                                       return type.getDeclaringClass()
                                           .map(JavaMetadataKeyIdModelParserUtils::keyIdResolverFromTypeOnSources).orElse(null);
                                     });
  }

  public static Optional<Pair<Integer, Boolean>> getMetadataKeyPart(ExtensionParameter extensionParameter) {
    return mapReduceSingleAnnotation(extensionParameter, "parameter", extensionParameter.getName(),
                                     org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart.class,
                                     MetadataKeyPart.class,
                                     value -> new Pair<>(value
                                         .getNumberValue(org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart::order),
                                                         value
                                                             .getBooleanValue(org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart::providedByKeyResolver)),
                                     value -> new Pair<>(value.getNumberValue(MetadataKeyPart::order),
                                                         value.getBooleanValue(MetadataKeyPart::providedByKeyResolver)));
  }


  private static JavaMetadataKeyModelParser keyIdResolverFromType(String parameterName, String categoryName,
                                                                  MetadataType metadataType, Class<?> clazz) {
    if (!isStaticResolver(clazz)) {
      return new JavaMetadataKeyModelParser(parameterName, categoryName, metadataType, clazz);
    }
    return null;
  }

  private static JavaMetadataKeyModelParser keyIdResolverFromTypeOnSources(Class<?> clazz) {
    if (!isStaticResolver(clazz)) {
      return new JavaMetadataKeyModelParser(null, null, null, clazz);
    }
    return null;
  }

  public static Optional<MetadataKeyModelParser> getKeyIdResolverModelParser(JavaOperationModelParser javaOperationModelParser,
                                                                             WithAnnotations operationType,
                                                                             ExtensionElement extensionElement) {
    return getKeyIdResolverModelParser(javaOperationModelParser.getOutputResolverModelParser().orElse(null),
                                       javaOperationModelParser.getAttributesResolverModelParser().orElse(null),
                                       javaOperationModelParser.getInputResolverModelParsers(),
                                       javaOperationModelParser.getParameterGroupModelParsers(),
                                       operationType,
                                       extensionElement,
                                       javaOperationModelParser.getName(),
                                       "operation");
  }

  public static Optional<MetadataKeyModelParser> getKeyIdResolverModelParser(JavaSourceModelParser javaSourceModelParser,
                                                                             WithAnnotations sourceType,
                                                                             ExtensionElement extensionElement) {
    return getKeyIdResolverModelParser(javaSourceModelParser.getOutputResolverModelParser().orElse(null),
                                       javaSourceModelParser.getAttributesResolverModelParser().orElse(null),
                                       Collections.emptyList(),
                                       javaSourceModelParser.getParameterGroupModelParsers(),
                                       sourceType,
                                       extensionElement,
                                       javaSourceModelParser.getName(),
                                       "source");
  }


  private static Optional<MetadataKeyModelParser> getKeyIdResolverModelParser(OutputResolverModelParser outputResolverModelParser,
                                                                              AttributesResolverModelParser attributesResolverModelParser,
                                                                              List<InputResolverModelParser> inputResolverModelParsers,
                                                                              List<ParameterGroupModelParser> parameterGroupModelParsers,
                                                                              WithAnnotations componentType,
                                                                              ExtensionElement extensionElement,
                                                                              String elementName,
                                                                              String elementType) {
    String categoryName = getCategoryName(outputResolverModelParser, attributesResolverModelParser, inputResolverModelParsers);

    Optional<MetadataKeyModelParser> keyIdResolverModelParser =
        (Optional<MetadataKeyModelParser>) parameterGroupModelParsers.stream()
            .map(parameterGroupModelParser -> {
              if (parameterGroupModelParser instanceof HasExtensionParameter) {
                return parseKeyIdResolverModelParser(((HasExtensionParameter) parameterGroupModelParser).getExtensionParameter(),
                                                     categoryName, parameterGroupModelParser.getName());
              }
              return empty();
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();

    if (!keyIdResolverModelParser.isPresent()) {
      keyIdResolverModelParser = (Optional<MetadataKeyModelParser>) parameterGroupModelParsers.stream()
          .map(ParameterGroupModelParser::getParameterParsers)
          .flatMap(List::stream)
          .map(parameterModelParser -> {

            if (parameterModelParser instanceof ParameterModelParserDecorator) {
              parameterModelParser = ((ParameterModelParserDecorator) parameterModelParser).getDecoratee();
            }
            if (parameterModelParser instanceof HasExtensionParameter) {
              return parseKeyIdResolverModelParser(((HasExtensionParameter) parameterModelParser).getExtensionParameter(),
                                                   categoryName, null);
            }
            return empty();
          })
          .filter(Optional::isPresent)
          .map(Optional::get)
          .findFirst();
    }

    if (keyIdResolverModelParser.isPresent() && !keyIdResolverModelParser.get().hasKeyIdResolver()) {
      Optional<MetadataKeyModelParser> enclosingKeyIdResolverModelParser =
          parseKeyIdResolverModelParser(extensionElement, componentType, elementType, elementName, extensionElement.getName());
      if (enclosingKeyIdResolverModelParser.isPresent()) {
        keyIdResolverModelParser = of(new JavaMetadataKeyModelParser(
                                                                     keyIdResolverModelParser.get().getParameterName(),
                                                                     categoryName,
                                                                     keyIdResolverModelParser.get().getMetadataType(),
                                                                     ((JavaMetadataKeyModelParser) enclosingKeyIdResolverModelParser
                                                                         .get())
                                                                             .keyIdResolverDeclarationClass()));
      }
    }

    if (keyIdResolverModelParser.isPresent()
        && outputResolverModelParser == null
        && attributesResolverModelParser == null
        && inputResolverModelParsers.isEmpty()) {
      // TODO W-14195099 - change this once we have `ProblemsReporter` available
      LOGGER
          .warn("A Keys Resolver is being defined without defining an Output Resolver, Input Resolver nor Attributes Resolver for element {} of extension {}",
                elementName, extensionElement.getName());
    }

    return keyIdResolverModelParser;
  }

  private static String getCategoryName(OutputResolverModelParser outputResolverModelParser,
                                        AttributesResolverModelParser attributesResolverModelParser,
                                        List<InputResolverModelParser> inputResolverModelParsers) {

    if (outputResolverModelParser != null) {
      return outputResolverModelParser.getOutputResolver().getCategoryName();
    }

    if (attributesResolverModelParser != null) {
      return attributesResolverModelParser.getAttributesResolver().getCategoryName();
    }

    for (InputResolverModelParser inputResolverModelParser : inputResolverModelParsers) {
      return inputResolverModelParser.getInputResolver().getCategoryName();
    }

    return null;
  }
}
