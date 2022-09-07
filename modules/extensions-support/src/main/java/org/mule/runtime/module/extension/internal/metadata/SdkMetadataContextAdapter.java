/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.sdk.api.metadata.MetadataCache;
import org.mule.sdk.api.metadata.MetadataContext;

import java.util.Optional;

public class SdkMetadataContextAdapter implements MetadataContext {

  private final org.mule.runtime.api.metadata.MetadataContext delegate;

  public SdkMetadataContextAdapter(org.mule.runtime.api.metadata.MetadataContext delegate) {
    this.delegate = delegate;
  }

  @Override
  public void dispose() {
    disposeIfNeeded(delegate);
  }

  @Override
  public <C> Optional<C> getConnection() throws ConnectionException {
    return delegate.getConnection();
  }

  @Override
  public ClassTypeLoader getTypeLoader() {
    return delegate.getTypeLoader();
  }

  @Override
  public BaseTypeBuilder getTypeBuilder() {
    return delegate.getTypeBuilder();
  }

  @Override
  public MetadataCache getCache() {
    return new SdkMetadataCacheAdapter(delegate.getCache());
  }
}
