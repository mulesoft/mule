/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.type.catalog;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.util.collection.SmallMap.of;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;

import java.util.Map;
import java.util.Optional;

/**
 * a {@link TypeLoader} for obtaining special types that aren't primitive types (such as string, number, etc.) nor defined in
 * other extensions.
 *
 * @since 4.5.0
 */
public class SpecialTypesTypeLoader implements TypeLoader {

  public static final String VOID = "void";
  public static final String ERROR = "error";

  private static final Map<String, MetadataType> SPECIAL_TYPES =
      unmodifiableMap(of(VOID, create(JAVA).voidType().build(), ERROR,
                         org.mule.runtime.extension.api.error.ErrorConstants.ERROR));

  @Override
  public Optional<MetadataType> load(String typeIdentifier) {
    return ofNullable(SPECIAL_TYPES.get(typeIdentifier));
  }
}
