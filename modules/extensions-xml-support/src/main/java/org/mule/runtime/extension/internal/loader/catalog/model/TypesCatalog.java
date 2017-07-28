/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.model;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.internal.loader.catalog.model.resolver.TypeResolver;

import java.util.List;
import java.util.Optional;

/**
 * Holds a collection of {@link TypeResolver} to look for any given type.
 * TODO MULE-13214: this class could be removed once MULE-13214 is done
 *
 * @since 4.0
 */
public class TypesCatalog {

  private final List<TypeResolver> typeResolvers;

  public TypesCatalog(List<TypeResolver> typeResolvers) {
    this.typeResolvers = typeResolvers;
  }

  public Optional<MetadataType> resolveType(String typeIdentifier) {
    return typeResolvers.stream()
        .map(typeResolver -> typeResolver.resolveType(typeIdentifier))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }
}
