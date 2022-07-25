/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.sdk.api.metadata.MetadataContext;
import org.mule.sdk.api.metadata.MetadataKey;
import org.mule.sdk.api.metadata.resolving.TypeKeysResolver;

import java.util.HashSet;
import java.util.Set;

/**
 * Adapts a {@link org.mule.runtime.api.metadata.resolving.TypeKeysResolver} into a {@link TypeKeysResolver}
 *
 * @since 4.5.0
 */
public class SdkTypeKeysResolverAdapter implements TypeKeysResolver {

  private final org.mule.runtime.api.metadata.resolving.TypeKeysResolver delegate;

  public SdkTypeKeysResolverAdapter(org.mule.runtime.api.metadata.resolving.TypeKeysResolver delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getCategoryName() {
    return delegate.getCategoryName();
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    Set<MetadataKey> metadataKeys = new HashSet<>();
    delegate.getKeys(new MuleMetadataContextAdapter(context))
        .forEach(metadataKey -> metadataKeys.add(new SdkMetadataKeyAdapter(metadataKey)));
    return metadataKeys;
  }

  @Override
  public String getResolverName() {
    return delegate.getResolverName();
  }

  public org.mule.runtime.api.metadata.resolving.TypeKeysResolver getDelegate() {
    return delegate;
  }
}
