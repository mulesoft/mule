/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.stream.Collectors.toSet;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link TypeKeysResolver} implementation which resolves automatically {@link MetadataKey}s for {@link Enum} based
 * MetadataKey Id parameters.
 *
 * @since 4.0
 * @see TypeKeysResolver
 */
public final class EnumKeyResolver implements TypeKeysResolver {

  private final Set<MetadataKey> keys;
  private final String categoryName;

  /**
   * @param anEnum An {@link Enum} represented by a {@link EnumAnnotation} of a {@link MetadataType}
   * @param categoryName Category name of the current {@link TypeKeysResolver}
   */
  EnumKeyResolver(EnumAnnotation anEnum, String categoryName) {
    keys = Stream.of(anEnum.getValues())
        .map(Object::toString)
        .map(MetadataKeyBuilder::newKey)
        .map(MetadataKeyBuilder::build)
        .collect(toSet());
    this.categoryName = categoryName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCategoryName() {
    return categoryName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return keys;
  }
}
