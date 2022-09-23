/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.type.catalog;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import org.mule.metadata.api.ListableTypeLoader;
import org.mule.metadata.api.annotation.TypeAliasAnnotation;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link ListableTypeLoader} which is able to load a {@link MetadataType} by identifier (if it has a
 * {@link TypeIdAnnotation}) or alias (if it has a {@link TypeAliasAnnotation}). The {@link #getAllTypes()} method will return the
 * corresponding dictionary with the identifier as a key (not the alias).
 *
 * @since 4.5.0
 */
public class ListableTypeLoaderByIdOrAlias implements ListableTypeLoader {

  private final Map<String, MetadataType> typeById;
  private final Map<String, MetadataType> typeByAlias;

  public ListableTypeLoaderByIdOrAlias(Collection<ObjectType> types) {
    typeById = new HashMap<>(types.size());
    typeByAlias = new HashMap<>(types.size());

    for (ObjectType type : types) {
      String id = getId(type);
      if (id != null) {
        typeById.put(id, type);
      }

      String alias = getAlias(type);
      if (alias != null) {
        typeByAlias.put(alias, type);
      }
    }
  }

  private static String getAlias(MetadataType type) {
    return type.getAnnotation(TypeAliasAnnotation.class).map(TypeAliasAnnotation::getValue).orElse(null);
  }

  private static String getId(MetadataType type) {
    return type.getAnnotation(TypeIdAnnotation.class).map(TypeIdAnnotation::getValue).orElse(null);
  }

  @Override
  public Map<String, MetadataType> getAllTypes() {
    return unmodifiableMap(typeById);
  }

  @Override
  public Optional<MetadataType> load(String typeIdOrAlias) {
    MetadataType metadataType = typeById.get(typeIdOrAlias);
    if (metadataType != null) {
      return of(metadataType);
    }

    return ofNullable(typeByAlias.get(typeIdOrAlias));
  }
}
