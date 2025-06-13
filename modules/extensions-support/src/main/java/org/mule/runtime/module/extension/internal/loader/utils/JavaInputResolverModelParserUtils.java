/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataTypeResolverUtils.isStaticResolver;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.extension.api.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.extension.api.loader.parser.ParameterModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.metadata.JavaInputResolverModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.metadata.chain.PassThroughChainInputTypeResolver;
import org.mule.sdk.api.annotation.metadata.ChainInputResolver;
import org.mule.sdk.api.annotation.metadata.PassThroughInputChainResolver;
import org.mule.sdk.api.annotation.metadata.TypeResolver;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

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

  /**
   * If defined, instantiates and returns the {@link ChainInputTypeResolver} of the given {@code chain}
   *
   * @param chain a {@link ExtensionParameter} representing the inner chain of a scope or route
   * @return an optional {@link ChainInputTypeResolver}
   * @since 4.7.0
   */
  public static Optional<ChainInputTypeResolver> getChainInputTypeResolver(ExtensionParameter chain) {
    Optional<ChainInputTypeResolver> resolver = chain.getValueFromAnnotation(PassThroughInputChainResolver.class)
        .map(a -> PassThroughChainInputTypeResolver.INSTANCE);

    if (!resolver.isPresent()) {
      resolver = chain.getValueFromAnnotation(ChainInputResolver.class)
          .flatMap(a -> a.getClassValue(ChainInputResolver::value).getDeclaringClass())
          .map(type -> {
            try {
              return (ChainInputTypeResolver) instantiateClass(type, null);
            } catch (Exception e) {
              throw new IllegalModelDefinitionException(format("Non instantiable %s type: %s",
                                                               ChainInputTypeResolver.class.getSimpleName(),
                                                               type.getName()),
                                                        e);
            }
          });
    }

    return resolver;
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
