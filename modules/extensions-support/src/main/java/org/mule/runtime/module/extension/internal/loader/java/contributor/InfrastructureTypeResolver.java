/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.contributor;

import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;

import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping;
import org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping.InfrastructureType;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

import java.util.Map;
import java.util.Optional;

/**
 * Resolves whether a {@link Type} is one of the considered as an Infrastructure Type ({@link InfrastructureTypeMapping})
 *
 * @since 4.4
 */
public class InfrastructureTypeResolver {

  private static final Map<Type, InfrastructureType> TYPE_MAPPING = InfrastructureTypeMapping.getMap().entrySet()
      .stream()
      .collect(toImmutableMap(entry -> new TypeWrapper(entry.getKey(),
                                                       new DefaultExtensionsTypeLoaderFactory()
                                                           .createTypeLoader(InfrastructureTypeMapping.class.getClassLoader())),
                              Map.Entry::getValue));


  public static Optional<InfrastructureType> getInfrastructureType(Type type) {
    return TYPE_MAPPING.entrySet()
        .stream()
        .filter(entry -> entry.getKey().isSameType(type))
        .map(Map.Entry::getValue)
        .findFirst();
  }
}

