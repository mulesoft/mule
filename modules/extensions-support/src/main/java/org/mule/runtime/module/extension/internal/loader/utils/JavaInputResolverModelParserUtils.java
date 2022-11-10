/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataTypeResolverUtils.isStaticResolver;

import org.mule.runtime.api.meta.model.declaration.fluent.SourceCallbackDeclaration;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaInputResolverModelParser;
import org.mule.sdk.api.annotation.metadata.TypeResolver;

import java.util.List;
import java.util.Optional;

/**
 * Helper class for introspecting input metadata.
 *
 * @since 4.5
 */
public class JavaInputResolverModelParserUtils {

  public static List<InputResolverModelParser> parseInputResolversModelParser(MethodElement<?> methodElement) {

    List<InputResolverModelParser> parameterInputResolvers = methodElement.getParameters().stream()
        .map(JavaInputResolverModelParserUtils::getResolverParser)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());

    List<InputResolverModelParser> parameterGroupInputResolvers = methodElement.getParameterGroups().stream()
        .map(JavaInputResolverModelParserUtils::getResolverParser)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());

    return concat(parameterInputResolvers.stream(), parameterGroupInputResolvers.stream()).collect(toList());
  }

  public static List<InputResolverModelParser> parseInputResolversModelParser(SourceCallbackDeclaration sourceCallbackDeclaration) {
    return sourceCallbackDeclaration.getAllParameters().stream()
        .map(param -> param.getModelProperty(ExtensionParameterDescriptorModelProperty.class))
        .filter(Optional::isPresent)
        .map(modelProperty -> modelProperty.get().getExtensionParameter())
        .map(JavaInputResolverModelParserUtils::getResolverParser)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  public static Optional<InputResolverModelParser> getResolverParser(ExtensionParameter extensionParameter) {
    Optional<Type> type = mapReduceSingleAnnotation(extensionParameter, "parameter", extensionParameter.getName(),
                                                    org.mule.runtime.extension.api.annotation.metadata.TypeResolver.class,
                                                    TypeResolver.class,
                                                    value -> value
                                                        .getClassValue(org.mule.runtime.extension.api.annotation.metadata.TypeResolver::value),
                                                    value -> value.getClassValue(TypeResolver::value));

    return type.flatMap(t -> getJavaInputResolverParser(extensionParameter.getName(), t));
  }

  private static Optional<InputResolverModelParser> getJavaInputResolverParser(String parameterName, Type type) {
    Class<?> resolverClazz;
    if (type.getDeclaringClass().isPresent()) {
      resolverClazz = type.getDeclaringClass().get();
    } else {
      return empty();
    }

    if (isStaticResolver(resolverClazz)) {
      return empty();
    }

    return of(new JavaInputResolverModelParser(parameterName, resolverClazz));
  }

}
