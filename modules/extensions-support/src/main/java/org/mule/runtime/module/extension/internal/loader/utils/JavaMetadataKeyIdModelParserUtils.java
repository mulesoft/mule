/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataTypeResolverUtils.isStaticResolver;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.parser.KeyIdResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaKeyIdResolverModelParser;
import org.mule.sdk.api.annotation.metadata.MetadataKeyId;
import org.mule.sdk.api.annotation.metadata.MetadataKeyPart;
import org.mule.sdk.api.annotation.metadata.MetadataScope;

import java.util.Optional;

/**
 * Helper class for introspecting metadata keys
 *
 * @since 4.5
 */
public class JavaMetadataKeyIdModelParserUtils {

  public static Optional<KeyIdResolverModelParser> parseKeyIdResolverModelParser(ExtensionParameter extensionParameter,
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

  public static Optional<KeyIdResolverModelParser> parseKeyIdResolverModelParser(Type extensionType,
                                                                                 WithAnnotations annotatedType) {
    Optional<KeyIdResolverModelParser> keyIdResolverModelParser = mapReduceSingleAnnotation(annotatedType, "source", "",
                                                                                            org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                                                            MetadataScope.class,
                                                                                            valueFetcher -> {
                                                                                              Type type = valueFetcher
                                                                                                  .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::keysResolver);
                                                                                              return type.getDeclaringClass()
                                                                                                  .map(JavaMetadataKeyIdModelParserUtils::keyIdResolverFromTypeOnSources)
                                                                                                  .orElse(null);
                                                                                            },
                                                                                            valueFetcher -> {
                                                                                              Type type = valueFetcher
                                                                                                  .getClassValue(MetadataScope::keysResolver);
                                                                                              return type.getDeclaringClass()
                                                                                                  .map(JavaMetadataKeyIdModelParserUtils::keyIdResolverFromTypeOnSources)
                                                                                                  .orElse(null);
                                                                                            });

    if (!keyIdResolverModelParser.isPresent()) {
      keyIdResolverModelParser = mapReduceSingleAnnotation(extensionType, "source", "",
                                                           org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                           MetadataScope.class,
                                                           valueFetcher -> {
                                                             Type type = valueFetcher
                                                                 .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::keysResolver);
                                                             Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                                             if (declaringClass != null && !isStaticResolver(declaringClass)) {
                                                               return keyIdResolverFromTypeOnSources(declaringClass);
                                                             } else {
                                                               return null;
                                                             }
                                                           },
                                                           valueFetcher -> {
                                                             Type type = valueFetcher.getClassValue(MetadataScope::keysResolver);
                                                             Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                                             if (declaringClass != null && !isStaticResolver(declaringClass)) {
                                                               return keyIdResolverFromTypeOnSources(declaringClass);
                                                             } else {
                                                               return null;
                                                             }
                                                           });
    }

    return keyIdResolverModelParser;
  }

  public static Optional<KeyIdResolverModelParser> parseKeyIdResolverModelParser(WithAnnotations annotatedType) {
    return mapReduceSingleAnnotation(annotatedType, "source", "",
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


  private static JavaKeyIdResolverModelParser keyIdResolverFromType(String parameterName, String categoryName,
                                                                    MetadataType metadataType, Class<?> clazz) {
    if (!isStaticResolver(clazz)) {
      return new JavaKeyIdResolverModelParser(parameterName, categoryName, metadataType, clazz);
    }
    return null;
  }

  private static JavaKeyIdResolverModelParser keyIdResolverFromTypeOnSources(Class<?> clazz) {
    if (!isStaticResolver(clazz)) {
      return new JavaKeyIdResolverModelParser(null, null, null, clazz);
    }
    return null;
  }

}
