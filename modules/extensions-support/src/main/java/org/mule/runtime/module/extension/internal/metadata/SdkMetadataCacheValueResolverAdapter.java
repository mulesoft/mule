/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.sdk.api.metadata.MetadataCache.MetadataCacheValueResolver;

import java.io.Serializable;

public class SdkMetadataCacheValueResolverAdapter implements MetadataCacheValueResolver {

  private final org.mule.runtime.api.metadata.MetadataCache.MetadataCacheValueResolver delegate;

  public SdkMetadataCacheValueResolverAdapter(org.mule.runtime.api.metadata.MetadataCache.MetadataCacheValueResolver delegate) {
    this.delegate = delegate;
  }

  @Override
  public Serializable compute(Serializable key) throws MetadataResolvingException, ConnectionException {
    return delegate.compute(key);
  }
}
