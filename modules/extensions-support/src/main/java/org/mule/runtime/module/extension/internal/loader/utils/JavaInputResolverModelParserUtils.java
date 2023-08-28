/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataTypeResolverUtils.isStaticResolver;

import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.parser.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParser;
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

  public static List<InputResolverModelParser> parseInputResolversModelParser(List<ParameterGroupModelParser> parameterGroupModelParsers) {
    return parameterGroupModelParsers.stream()
        .flatMap(parameterGroup -> parameterGroup.getParameterParsers().stream())
        .map(ParameterModelParser::getInputResolverModelParser)
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
