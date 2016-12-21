/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.Collections.unmodifiableSet;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link TypeKeysResolver} implementation which resolves automatically {@link MetadataKey}s for boolean based
 * MetadataKey Id parameters.
 * </p>
 * This resolver will only return the possible values of a boolean: {@code true} and {@code false}
 *
 * @since 4.0
 * @see TypeKeysResolver
 */
public final class BooleanKeyResolver implements TypeKeysResolver {

  private final static Set<MetadataKey> keys = new HashSet<MetadataKey>() {

    {
      add(newKey("TRUE").build());
      add(newKey("FALSE").build());
    }
  };
  private final String categoryName;

  BooleanKeyResolver(String categoryName) {
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
    return unmodifiableSet(keys);
  }
}
