/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.contributor;

import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;

import static java.lang.String.format;
import static java.util.function.Function.identity;

import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.util.InfrastructureTypeUtils;
import org.mule.runtime.extension.api.loader.util.InfrastructureTypeUtils.InfrastructureType;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

import java.util.Map;
import java.util.Optional;

/**
 * Resolves whether a {@link Type} is one of the considered as an Infrastructure Type.
 *
 * @since 4.4
 */
public class InfrastructureTypeResolver {

  private static final Map<Type, InfrastructureType> TYPE_MAPPING = InfrastructureTypeUtils.getInfrastructureTypes()
      .stream()
      .collect(toImmutableMap(infrastructureType -> new TypeWrapper(infrastructureType.getClazz()
          .orElseThrow(() -> new RuntimeException(format("Expected infrastructure type to have an associated class: '%s'",
                                                         infrastructureType))),
                                                                    new DefaultExtensionsTypeLoaderFactory()
                                                                        .createTypeLoader(InfrastructureTypeUtils.class
                                                                            .getClassLoader())),
                              identity()));


  public static Optional<InfrastructureType> getInfrastructureType(Type type) {
    return TYPE_MAPPING.entrySet()
        .stream()
        .filter(entry -> entry.getKey().isSameType(type))
        .map(Map.Entry::getValue)
        .findFirst();
  }
}

