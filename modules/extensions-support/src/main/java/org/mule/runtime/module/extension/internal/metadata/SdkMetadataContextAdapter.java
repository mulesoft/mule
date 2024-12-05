/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.sdk.api.metadata.MetadataCache;
import org.mule.sdk.api.metadata.MetadataContext;
import org.mule.sdk.api.metadata.RouterOutputMetadataContext;
import org.mule.sdk.api.metadata.ScopeOutputMetadataContext;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Adapts {@code mule-api}'s {@link org.mule.runtime.api.metadata.MetadataContext} into a {@code sdk-api} {@link MetadataContext}
 *
 * @since 4.7.0
 */
public class SdkMetadataContextAdapter implements MetadataContext {

  private final org.mule.runtime.api.metadata.MetadataContext delegate;

  public SdkMetadataContextAdapter(org.mule.runtime.api.metadata.MetadataContext delegate) {
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
  public MetadataCache getCache() {
    return new SdkMetadataCacheAdapter(delegate.getCache());
  }

  @Override
  public Optional<ScopeOutputMetadataContext> getScopeOutputMetadataContext() {
    return delegate.getScopeOutputMetadataContext().map(SdkScopeOutputMetadataContext::new);
  }

  @Override
  public Optional<RouterOutputMetadataContext> getRouterOutputMetadataContext() {
    return delegate.getRouterOutputMetadataContext().map(SdkRouterOutputMetadataContextAdapter::new);
  }

  static class SdkScopeOutputMetadataContext implements ScopeOutputMetadataContext {

    private final org.mule.runtime.api.metadata.ScopeOutputMetadataContext delegate;

    private SdkScopeOutputMetadataContext(org.mule.runtime.api.metadata.ScopeOutputMetadataContext delegate) {
      this.delegate = delegate;
    }

    @Override
    public Supplier<MessageMetadataType> getInnerChainOutputMessageType() {
      return delegate.getInnerChainOutputMessageType();
    }

    @Override
    public Supplier<MessageMetadataType> getScopeInputMessageType() {
      return delegate.getScopeInputMessageType();
    }

    org.mule.runtime.api.metadata.ScopeOutputMetadataContext getDelegate() {
      return delegate;
    }
  }

  static class SdkRouterOutputMetadataContextAdapter implements RouterOutputMetadataContext {

    private final org.mule.runtime.api.metadata.RouterOutputMetadataContext delegate;

    public SdkRouterOutputMetadataContextAdapter(org.mule.runtime.api.metadata.RouterOutputMetadataContext delegate) {
      this.delegate = delegate;
    }

    @Override
    public Map<String, Supplier<MessageMetadataType>> getRouteOutputMessageTypes() {
      return delegate.getRouteOutputMessageTypes();
    }

    @Override
    public Supplier<MessageMetadataType> getRouterInputMessageType() {
      return delegate.getRouterInputMessageType();
    }

    org.mule.runtime.api.metadata.RouterOutputMetadataContext getDelegate() {
      return delegate;
    }
  }
}
