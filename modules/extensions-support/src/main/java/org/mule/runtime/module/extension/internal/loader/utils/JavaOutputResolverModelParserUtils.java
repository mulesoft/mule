/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;

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

  public static JavaOutputResolverModelParser parseOutputResolverModelParser(MethodElement<?> methodElement) {
    return mapReduceSingleAnnotation(methodElement, "operation", methodElement.getName(),
                                     org.mule.runtime.extension.api.annotation.metadata.OutputResolver.class,
                                     OutputResolver.class,
                                     value -> outputResolverFromType(value
                                         .getClassValue(org.mule.runtime.extension.api.annotation.metadata.OutputResolver::output),
                                                                     true),
                                     value -> outputResolverFromType(value.getClassValue(OutputResolver::output), false))
                                         .orElse(new JavaOutputResolverModelParser(NullMetadataResolver.class, false));
  }

  public static JavaOutputResolverModelParser parseOutputResolverModelParser(Type extensionType, WithAnnotations annotatedType) {
    Optional<JavaOutputResolverModelParser> javaOutputResolverModelParse = mapReduceSingleAnnotation(annotatedType, "source", "",
                                                                                                     org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                                                                     MetadataScope.class,
                                                                                                     value -> outputResolverFromType(value
                                                                                                         .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::outputResolver),
                                                                                                                                     true),
                                                                                                     value -> outputResolverFromType(value
                                                                                                         .getClassValue(MetadataScope::outputResolver),
                                                                                                                                     false));

    if (!javaOutputResolverModelParse.isPresent()) {
      javaOutputResolverModelParse = mapReduceSingleAnnotation(extensionType, "source", "",
                                                               org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                               MetadataScope.class,
                                                               value -> outputResolverFromType(value
                                                                   .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::outputResolver),
                                                                                               true),
                                                               value -> outputResolverFromType(value
                                                                   .getClassValue(MetadataScope::outputResolver),
                                                                                               false));
    }

    return javaOutputResolverModelParse.orElse(new JavaOutputResolverModelParser(NullMetadataResolver.class, false));
  }

  public static JavaAttributesResolverModelParser parseAttributesResolverModelParser(MethodElement<?> methodElement) {
    return mapReduceSingleAnnotation(methodElement, "operation", methodElement.getName(),
                                     org.mule.runtime.extension.api.annotation.metadata.OutputResolver.class,
                                     OutputResolver.class,
                                     value -> attributesResolverFromType(value
                                         .getClassValue(org.mule.runtime.extension.api.annotation.metadata.OutputResolver::attributes),
                                                                         true),
                                     value -> attributesResolverFromType(value.getClassValue(OutputResolver::attributes), false))
                                         .orElse(new JavaAttributesResolverModelParser(NullMetadataResolver.class, false));
  }

  public static JavaAttributesResolverModelParser parseAttributesResolverModelParser(Type extensionType,
                                                                                     WithAnnotations annotatedType) {
    Optional<JavaAttributesResolverModelParser> javaAttributesResolverModelParser =
        mapReduceSingleAnnotation(annotatedType, "source", "",
                                  org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                  MetadataScope.class,
                                  value -> attributesResolverFromType(value
                                      .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::attributesResolver),
                                                                      true),
                                  value -> attributesResolverFromType(value
                                      .getClassValue(MetadataScope::attributesResolver),
                                                                      false));

    if (!javaAttributesResolverModelParser.isPresent()) {
      javaAttributesResolverModelParser = mapReduceSingleAnnotation(extensionType, "source", "",
                                                                    org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                                    MetadataScope.class,
                                                                    value -> attributesResolverFromType(value
                                                                        .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::outputResolver),
                                                                                                        true),
                                                                    value -> attributesResolverFromType(value
                                                                        .getClassValue(MetadataScope::outputResolver),
                                                                                                        false));
    }

    return javaAttributesResolverModelParser.orElse(new JavaAttributesResolverModelParser(NullMetadataResolver.class, false));
  }

  private static JavaOutputResolverModelParser outputResolverFromType(Type type, boolean muleResolver) {
    return new JavaOutputResolverModelParser(type.getDeclaringClass().get(), muleResolver);
  }

  private static JavaAttributesResolverModelParser attributesResolverFromType(Type type, boolean muleResolver) {
    return new JavaAttributesResolverModelParser(type.getDeclaringClass().get(), muleResolver);
  }
}
