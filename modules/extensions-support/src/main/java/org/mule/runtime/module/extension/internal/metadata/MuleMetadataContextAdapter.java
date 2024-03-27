/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.ScopeOutputMetadataContext;
import org.mule.runtime.api.metadata.RouterOutputMetadataContext;
import org.mule.sdk.api.metadata.MetadataContext;

import java.util.Optional;

public class MuleMetadataContextAdapter implements org.mule.runtime.api.metadata.MetadataContext {

  private final MetadataContext delegate;

  public MuleMetadataContextAdapter(MetadataContext delegate) {
    this.delegate = delegate;
  }

  @Override
  public void dispose() {
    delegate.dispose();
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
  public org.mule.runtime.api.metadata.MetadataCache getCache() {
    return new MuleMetadataCacheAdapter(delegate.getCache());
  }

  @Override
  public Optional<ScopeOutputMetadataContext> getScopeOutputMetadataContext() {
    return delegate.getScopeChainPropagationContext()
        .map(ctx -> ((SdkMetadataContextAdapter.SdkChainPropagationContextAdapter) ctx).getDelegate());
  }

  @Override
  public Optional<RouterOutputMetadataContext> getRouterOutputMetadataContext() {
    return delegate.getRouterPropagationContext()
        .map(ctx -> ((SdkMetadataContextAdapter.SdkRouterPropagationContextAdapter) ctx).getDelegate());
  }

  @Override
  public <C> Optional<C> getConfig() {
    return Optional.empty();
  }
}
