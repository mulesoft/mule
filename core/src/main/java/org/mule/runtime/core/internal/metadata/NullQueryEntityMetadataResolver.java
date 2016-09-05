/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import static java.util.Collections.emptySet;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;

import java.util.Set;

/**
 * Null implementation of a {@link QueryEntityResolver}, returns an emptySet of {@code entityKeys} and
 * {@code null} when the {@link QueryEntityResolver#getEntityMetadata(MetadataContext, String)} method is invoked.
 *
 * @since 4.0
 */
class NullQueryEntityMetadataResolver implements QueryEntityResolver {

  /**
   * {@inheritDoc}
   *
   * @return an empty {@link Set}.
   */
  @Override
  public Set<MetadataKey> getEntityKeys(MetadataContext context)
      throws MetadataResolvingException, ConnectionException {
    return emptySet();
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code null}.
   */
  @Override
  public MetadataType getEntityMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    //TODO - MDM-21: change when VoidType is added to the MDM
    return null;
  }
}
