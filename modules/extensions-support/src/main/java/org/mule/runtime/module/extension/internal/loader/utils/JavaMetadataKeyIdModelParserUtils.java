/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static java.util.Optional.empty;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaKeyIdResolverModelParser;
import org.mule.sdk.api.annotation.metadata.MetadataKeyId;
import org.mule.sdk.api.annotation.metadata.MetadataScope;
import org.mule.sdk.api.metadata.NullMetadataResolver;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper class for introspecting metadata keys
 *
 * @since 4.5
 */
public class JavaMetadataKeyIdModelParserUtils {

  public static JavaKeyIdResolverModelParser parseKeyIdResolverModelParser(Supplier<String> categoryName,
                                                                           MethodElement<?> methodElement) {
    Optional<JavaKeyIdResolverModelParser> javaKeyIdResolverModelParser = empty();

    Optional<ExtensionParameter> extensionParameter = methodElement.getParameters().stream()
        .filter(param -> param.isAnnotatedWith(MetadataKeyId.class) ||
            param.isAnnotatedWith(org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId.class))
        .findFirst();

    if (extensionParameter.isPresent()) {
      MetadataType metadataType = extensionParameter.get().getType().asMetadataType();
      javaKeyIdResolverModelParser =
          mapReduceSingleAnnotation(extensionParameter.get(), "operation", extensionParameter.get().getName(),
                                    org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId.class,
                                    MetadataKeyId.class,
                                    value -> keyIdResolverFromType(categoryName.get(), metadataType, value
                                        .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId::value),
                                                                   true),
                                    value -> keyIdResolverFromType(categoryName.get(), metadataType,
                                                                   value.getClassValue(MetadataKeyId::value),
                                                                   false));
    } else {
      Optional<ExtensionParameter> groupExtensionParameter = methodElement.getParameterGroups().stream()
          .filter(param -> param.isAnnotatedWith(MetadataKeyId.class) ||
              param.isAnnotatedWith(org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId.class))
          .findFirst();

      if (groupExtensionParameter.isPresent()) {
        MetadataType metadataType = groupExtensionParameter.get().getType().asMetadataType();
        javaKeyIdResolverModelParser =
            mapReduceSingleAnnotation(groupExtensionParameter.get(), "operation", groupExtensionParameter.get().getName(),
                                      org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId.class,
                                      MetadataKeyId.class,
                                      value -> keyIdResolverFromType(categoryName.get(), metadataType, value
                                          .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId::value),
                                                                     true),
                                      value -> keyIdResolverFromType(categoryName.get(), metadataType,
                                                                     value.getClassValue(MetadataKeyId::value),
                                                                     false));
      }

    }

    return javaKeyIdResolverModelParser
        .orElseGet(() -> new JavaKeyIdResolverModelParser(null, null, NullMetadataResolver.class, false));
  }

  public static JavaKeyIdResolverModelParser parseKeyIdResolverModelParser(Type extensionType, WithAnnotations annotatedType) {
    Optional<JavaKeyIdResolverModelParser> javaKeyIdResolverModelParser = mapReduceSingleAnnotation(annotatedType, "source", "",
                                                                                                    org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                                                                    MetadataScope.class,
                                                                                                    value -> keyIdResolverFromType(null,
                                                                                                                                   null,
                                                                                                                                   value
                                                                                                                                       .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::keysResolver),
                                                                                                                                   true),
                                                                                                    value -> keyIdResolverFromType(null,
                                                                                                                                   null,
                                                                                                                                   value
                                                                                                                                       .getClassValue(MetadataScope::keysResolver),
                                                                                                                                   false));

    if (!javaKeyIdResolverModelParser.isPresent()) {
      javaKeyIdResolverModelParser = mapReduceSingleAnnotation(extensionType, "source", "",
                                                               org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                               MetadataScope.class,
                                                               value -> keyIdResolverFromType(null, null, value
                                                                   .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::keysResolver),
                                                                                              true),
                                                               value -> keyIdResolverFromType(null, null, value
                                                                   .getClassValue(MetadataScope::keysResolver),
                                                                                              false));
    }

    return javaKeyIdResolverModelParser
        .orElse(new JavaKeyIdResolverModelParser(null, null, NullMetadataResolver.class, false));
  }

  private static JavaKeyIdResolverModelParser keyIdResolverFromType(String categoryName, MetadataType metadataType, Type type,
                                                                    boolean muleResolver) {
    return new JavaKeyIdResolverModelParser(categoryName, metadataType, type.getDeclaringClass().get(), muleResolver);
  }

}
