/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;

import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaAttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaOutputResolverModelParser;
import org.mule.sdk.api.annotation.metadata.MetadataScope;
import org.mule.sdk.api.annotation.metadata.OutputResolver;
import org.mule.sdk.api.metadata.NullMetadataResolver;

import java.util.Optional;

/**
 * Helper class for introspecting output and attributes metadata.
 *
 * @since 4.5
 */
public class JavaOutputResolverModelParserUtils {

  public static boolean hasOutputResolverAnnotation(MethodElement<?> methodElement) {
    return methodElement.isAnnotatedWith(org.mule.runtime.extension.api.annotation.metadata.OutputResolver.class) ||
        methodElement.isAnnotatedWith(OutputResolver.class);
  }

  public static boolean hasMetadataScopeAnnotation(Class<?> type) {
    return getAnnotation(type, org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class) != null ||
        getAnnotation(type, MetadataScope.class) != null;
  }

  public static JavaOutputResolverModelParser parseOutputResolverModelParser(MethodElement<?> methodElement) {
    return mapReduceSingleAnnotation(methodElement, "operation", methodElement.getName(),
                                     org.mule.runtime.extension.api.annotation.metadata.OutputResolver.class,
                                     OutputResolver.class,
                                     value -> outputResolverFromType(value
                                         .getClassValue(org.mule.runtime.extension.api.annotation.metadata.OutputResolver::output)),
                                     value -> outputResolverFromType(value.getClassValue(OutputResolver::output)))
                                         .orElse(new JavaOutputResolverModelParser(NullMetadataResolver.class));
  }

  public static JavaOutputResolverModelParser parseOutputResolverModelParser(Type extensionType, WithAnnotations annotatedType) {
    Optional<JavaOutputResolverModelParser> javaOutputResolverModelParse = mapReduceSingleAnnotation(annotatedType, "source", "",
                                                                                                     org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                                                                     MetadataScope.class,
                                                                                                     value -> outputResolverFromType(value
                                                                                                         .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::outputResolver)),
                                                                                                     value -> outputResolverFromType(value
                                                                                                         .getClassValue(MetadataScope::outputResolver)));

    if (!javaOutputResolverModelParse.isPresent()) {
      javaOutputResolverModelParse = mapReduceSingleAnnotation(extensionType, "source", "",
                                                               org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                               MetadataScope.class,
                                                               value -> outputResolverFromType(value
                                                                   .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::outputResolver)),
                                                               value -> outputResolverFromType(value
                                                                   .getClassValue(MetadataScope::outputResolver)));
    }

    return javaOutputResolverModelParse.orElse(new JavaOutputResolverModelParser(NullMetadataResolver.class));
  }

  public static JavaAttributesResolverModelParser parseAttributesResolverModelParser(MethodElement<?> methodElement) {
    return mapReduceSingleAnnotation(methodElement, "operation", methodElement.getName(),
                                     org.mule.runtime.extension.api.annotation.metadata.OutputResolver.class,
                                     OutputResolver.class,
                                     value -> attributesResolverFromType(value
                                         .getClassValue(org.mule.runtime.extension.api.annotation.metadata.OutputResolver::attributes)),
                                     value -> attributesResolverFromType(value.getClassValue(OutputResolver::attributes)))
                                         .orElse(new JavaAttributesResolverModelParser(NullMetadataResolver.class));
  }

  public static JavaAttributesResolverModelParser parseAttributesResolverModelParser(Type extensionType,
                                                                                     WithAnnotations annotatedType) {
    Optional<JavaAttributesResolverModelParser> javaAttributesResolverModelParser =
        mapReduceSingleAnnotation(annotatedType, "source", "",
                                  org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                  MetadataScope.class,
                                  value -> attributesResolverFromType(value
                                      .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::attributesResolver)),
                                  value -> attributesResolverFromType(value
                                      .getClassValue(MetadataScope::attributesResolver)));

    if (!javaAttributesResolverModelParser.isPresent()) {
      javaAttributesResolverModelParser = mapReduceSingleAnnotation(extensionType, "source", "",
                                                                    org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                                    MetadataScope.class,
                                                                    value -> attributesResolverFromType(value
                                                                        .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::outputResolver)),
                                                                    value -> attributesResolverFromType(value
                                                                        .getClassValue(MetadataScope::outputResolver)));
    }

    return javaAttributesResolverModelParser.orElse(new JavaAttributesResolverModelParser(NullMetadataResolver.class));
  }

  private static JavaOutputResolverModelParser outputResolverFromType(Type type) {
    return new JavaOutputResolverModelParser(type.getDeclaringClass().get());
  }

  private static JavaAttributesResolverModelParser attributesResolverFromType(Type type) {
    return new JavaAttributesResolverModelParser(type.getDeclaringClass().get());
  }
}
