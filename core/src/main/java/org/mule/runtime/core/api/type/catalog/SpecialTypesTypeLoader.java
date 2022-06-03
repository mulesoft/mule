/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.type.catalog;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * a {@link TypeLoader} for obtaining special types that aren't primitive types (such as string, number, etc.) nor defined in
 * other extensions.
 *
 * @since 4.5.0
 */
public class SpecialTypesTypeLoader implements TypeLoader {

  private static final Map<String, MetadataType> SPECIAL_TYPES;
  public static final String VOID = "void";

  static {
    Map<String, MetadataType> types = new HashMap<>(1);
    types.put(VOID, create(JAVA).voidType().build());
    SPECIAL_TYPES = unmodifiableMap(types);
  }

  @Override
  public Optional<MetadataType> load(String typeIdentifier) {
    return ofNullable(SPECIAL_TYPES.get(typeIdentifier));
  }
}
