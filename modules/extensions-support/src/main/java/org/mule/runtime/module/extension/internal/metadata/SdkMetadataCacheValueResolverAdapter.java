/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
