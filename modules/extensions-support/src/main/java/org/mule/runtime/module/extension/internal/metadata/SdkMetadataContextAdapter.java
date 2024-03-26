/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.sdk.api.metadata.ChainPropagationContext;
import org.mule.sdk.api.metadata.MetadataCache;
import org.mule.sdk.api.metadata.MetadataContext;
import org.mule.sdk.api.metadata.RouterPropagationContext;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

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
  public Optional<ChainPropagationContext> getScopeChainPropagationContext() {
    return delegate.getScopePropagationContext().map(SdkChainPropagationContextAdapter::new);
  }

  @Override
  public Optional<RouterPropagationContext> getRouterPropagationContext() {
    return delegate.getRouterPropagationContext().map(SdkRouterPropagationContextAdapter::new);
  }

  static class SdkChainPropagationContextAdapter implements ChainPropagationContext {

    private final org.mule.runtime.api.metadata.ChainPropagationContext delegate;

    private SdkChainPropagationContextAdapter(org.mule.runtime.api.metadata.ChainPropagationContext delegate) {
      this.delegate = delegate;
    }

    @Override
    public Supplier<MessageMetadataType> getChainInputResolver() {
      return delegate.getChainInputResolver();
    }

    @Override
    public Supplier<MessageMetadataType> getChainOutputResolver() {
      return delegate.getChainOutputResolver();
    }

    org.mule.runtime.api.metadata.ChainPropagationContext getDelegate() {
      return delegate;
    }
  }

  static class SdkRouterPropagationContextAdapter implements RouterPropagationContext {

    private final org.mule.runtime.api.metadata.RouterPropagationContext delegate;

    private SdkRouterPropagationContextAdapter(org.mule.runtime.api.metadata.RouterPropagationContext delegate) {
      this.delegate = delegate;
    }

    @Override
    public Map<String, ChainPropagationContext> getRoutesPropagationContext() {
      return unmodifiableMap(
                             delegate.getRoutesPropagationContext().entrySet().stream().collect(
                                                                                                toMap(entry -> entry.getKey(),
                                                                                                      entry -> new SdkChainPropagationContextAdapter(entry
                                                                                                          .getValue()))));
    }

    org.mule.runtime.api.metadata.RouterPropagationContext getDelegate() {
      return delegate;
    }
  }
}
