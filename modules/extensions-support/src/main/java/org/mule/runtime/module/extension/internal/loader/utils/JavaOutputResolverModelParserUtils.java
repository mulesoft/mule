/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataTypeResolverUtils.isStaticResolver;

import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaAttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaOutputResolverModelParser;
import org.mule.sdk.api.annotation.metadata.MetadataScope;
import org.mule.sdk.api.annotation.metadata.OutputResolver;

import java.util.Optional;

/**
 * Helper class for introspecting output and attributes metadata.
 *
 * @since 4.5
 */
public class JavaOutputResolverModelParserUtils {

  public static Optional<OutputResolverModelParser> parseOutputResolverModelParser(MethodElement<?> methodElement) {
    return mapReduceSingleAnnotation(methodElement, "operation", methodElement.getName(),
                                     org.mule.runtime.extension.api.annotation.metadata.OutputResolver.class,
                                     OutputResolver.class,
                                     valueFetcher -> {
                                       Type type = valueFetcher
                                           .getClassValue(org.mule.runtime.extension.api.annotation.metadata.OutputResolver::output);
                                       Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                       if (declaringClass != null && !isStaticResolver(declaringClass)) {
                                         return outputResolverFromType(declaringClass);
                                       } else {
                                         return null;
                                       }
                                     },
                                     valueFetcher -> {
                                       Type type = valueFetcher.getClassValue(OutputResolver::output);
                                       Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                       if (declaringClass != null && !isStaticResolver(declaringClass)) {
                                         return outputResolverFromType(declaringClass);
                                       } else {
                                         return null;
                                       }
                                     });
  }

  public static Optional<OutputResolverModelParser> parseOutputResolverModelParser(Type extensionType,
                                                                                   WithAnnotations annotatedType) {
    Optional<OutputResolverModelParser> javaOutputResolverModelParse = mapReduceSingleAnnotation(annotatedType, "source", "",
                                                                                                 org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                                                                 MetadataScope.class,
                                                                                                 valueFetcher -> {
                                                                                                   Type type = valueFetcher
                                                                                                       .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::outputResolver);
                                                                                                   Class<?> declaringClass =
                                                                                                       type.getDeclaringClass()
                                                                                                           .orElse(null);
                                                                                                   if (declaringClass != null
                                                                                                       && !isStaticResolver(declaringClass)) {
                                                                                                     return outputResolverFromType(declaringClass);
                                                                                                   } else {
                                                                                                     return null;
                                                                                                   }
                                                                                                 },
                                                                                                 valueFetcher -> {
                                                                                                   Type type = valueFetcher
                                                                                                       .getClassValue(MetadataScope::outputResolver);
                                                                                                   Class<?> declaringClass =
                                                                                                       type.getDeclaringClass()
                                                                                                           .orElse(null);
                                                                                                   if (declaringClass != null
                                                                                                       && !isStaticResolver(declaringClass)) {
                                                                                                     return outputResolverFromType(declaringClass);
                                                                                                   } else {
                                                                                                     return null;
                                                                                                   }
                                                                                                 });

    if (!javaOutputResolverModelParse.isPresent()) {
      javaOutputResolverModelParse = mapReduceSingleAnnotation(extensionType, "source", "",
                                                               org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                               MetadataScope.class,
                                                               valueFetcher -> {
                                                                 Type type = valueFetcher
                                                                     .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::outputResolver);
                                                                 Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                                                 if (declaringClass != null
                                                                     && !isStaticResolver(declaringClass)) {
                                                                   return outputResolverFromType(declaringClass);
                                                                 } else {
                                                                   return null;
                                                                 }
                                                               },
                                                               valueFetcher -> {
                                                                 Type type =
                                                                     valueFetcher.getClassValue(MetadataScope::outputResolver);
                                                                 Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                                                 if (declaringClass != null
                                                                     && !isStaticResolver(declaringClass)) {
                                                                   return outputResolverFromType(declaringClass);
                                                                 } else {
                                                                   return null;
                                                                 }
                                                               });
    }

    return javaOutputResolverModelParse;
  }

  public static Optional<AttributesResolverModelParser> parseAttributesResolverModelParser(MethodElement<?> methodElement) {
    return mapReduceSingleAnnotation(methodElement, "operation", methodElement.getName(),
                                     org.mule.runtime.extension.api.annotation.metadata.OutputResolver.class,
                                     OutputResolver.class,
                                     valueFetcher -> {
                                       Type type = valueFetcher
                                           .getClassValue(org.mule.runtime.extension.api.annotation.metadata.OutputResolver::attributes);
                                       Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                       if (declaringClass != null && !isStaticResolver(declaringClass)) {
                                         return attributesResolverFromType(declaringClass);
                                       } else {
                                         return null;
                                       }
                                     },
                                     valueFetcher -> {
                                       Type type = valueFetcher.getClassValue(OutputResolver::attributes);
                                       Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                       if (declaringClass != null && !isStaticResolver(declaringClass)) {
                                         return attributesResolverFromType(declaringClass);
                                       } else {
                                         return null;
                                       }
                                     });
  }

  public static Optional<AttributesResolverModelParser> parseAttributesResolverModelParser(Type extensionType,
                                                                                           WithAnnotations annotatedType) {
    Optional<AttributesResolverModelParser> attributesResolverModelParser =
        mapReduceSingleAnnotation(annotatedType, "source", "",
                                  org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                  MetadataScope.class,
                                  valueFetcher -> {
                                    Type type = valueFetcher
                                        .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::attributesResolver);
                                    Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                    if (declaringClass != null && !isStaticResolver(declaringClass)) {
                                      return attributesResolverFromType(declaringClass);
                                    } else {
                                      return null;
                                    }
                                  },
                                  valueFetcher -> {
                                    Type type = valueFetcher.getClassValue(MetadataScope::attributesResolver);
                                    Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                    if (declaringClass != null && !isStaticResolver(declaringClass)) {
                                      return attributesResolverFromType(declaringClass);
                                    } else {
                                      return null;
                                    }
                                  });

    if (!attributesResolverModelParser.isPresent()) {
      attributesResolverModelParser = mapReduceSingleAnnotation(extensionType, "source", "",
                                                                org.mule.runtime.extension.api.annotation.metadata.MetadataScope.class,
                                                                MetadataScope.class,
                                                                valueFetcher -> {
                                                                  Type type = valueFetcher
                                                                      .getClassValue(org.mule.runtime.extension.api.annotation.metadata.MetadataScope::outputResolver);
                                                                  Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                                                  if (declaringClass != null
                                                                      && !isStaticResolver(declaringClass)) {
                                                                    return attributesResolverFromType(declaringClass);
                                                                  } else {
                                                                    return null;
                                                                  }
                                                                },
                                                                valueFetcher -> {
                                                                  Type type =
                                                                      valueFetcher.getClassValue(MetadataScope::outputResolver);
                                                                  Class<?> declaringClass = type.getDeclaringClass().orElse(null);
                                                                  if (declaringClass != null
                                                                      && !isStaticResolver(declaringClass)) {
                                                                    return attributesResolverFromType(declaringClass);
                                                                  } else {
                                                                    return null;
                                                                  }
                                                                });
    }

    return attributesResolverModelParser;
  }

  private static JavaOutputResolverModelParser outputResolverFromType(Class<?> clazz) {
    return new JavaOutputResolverModelParser(clazz);
  }

  private static JavaAttributesResolverModelParser attributesResolverFromType(Class<?> clazz) {
    return new JavaAttributesResolverModelParser(clazz);
  }

}
