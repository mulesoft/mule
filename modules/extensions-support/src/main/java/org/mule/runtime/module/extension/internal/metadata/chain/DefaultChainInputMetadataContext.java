/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata.chain;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.module.extension.internal.metadata.SdkMetadataContextAdapter;
import org.mule.sdk.api.metadata.ChainInputMetadataContext;
import org.mule.sdk.api.metadata.MetadataCache;
import org.mule.sdk.api.metadata.MetadataContext;
import org.mule.sdk.api.metadata.RouterOutputMetadataContext;
import org.mule.sdk.api.metadata.ScopeOutputMetadataContext;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default implementation of {@link ChainInputMetadataContext}
 *
 * @since 4.7.0
 */
public class DefaultChainInputMetadataContext implements ChainInputMetadataContext {

  private final Supplier<MessageMetadataType> inputMessageMetadataType;
  private final InputMetadataDescriptor inputMetadataDescriptor;
  private final MetadataContext rootContext;

  public DefaultChainInputMetadataContext(Supplier<MessageMetadataType> inputMessageMetadataType,
                                          InputMetadataDescriptor inputMetadataDescriptor,
                                          MetadataContext rootContext) {
    this.inputMessageMetadataType = new LazyValue<>(inputMessageMetadataType);
    this.inputMetadataDescriptor = inputMetadataDescriptor;
    this.rootContext = rootContext;
  }

  public DefaultChainInputMetadataContext(Supplier<MessageMetadataType> inputMessageMetadataType,
                                          InputMetadataDescriptor inputMetadataDescriptor,
                                          org.mule.runtime.api.metadata.MetadataContext rootContext) {
    this(inputMessageMetadataType, inputMetadataDescriptor, new SdkMetadataContextAdapter(rootContext));
  }

  @Override
  public MetadataType getParameterResolvedType(String parameterName) throws NoSuchElementException {
    try {
      return inputMetadataDescriptor.getParameterMetadata(parameterName).getType();
    } catch (IllegalArgumentException e) {
      throw new NoSuchElementException(e.getMessage());
    }
  }

  @Override
  public MessageMetadataType getInputMessageMetadataType() {
    return inputMessageMetadataType.get();
  }

  @Override
  public void dispose() {
    rootContext.dispose();
  }

  @Override
  public <C> Optional<C> getConnection() throws ConnectionException {
    return rootContext.getConnection();
  }

  @Override
  public ClassTypeLoader getTypeLoader() {
    return rootContext.getTypeLoader();
  }

  @Override
  public BaseTypeBuilder getTypeBuilder() {
    return rootContext.getTypeBuilder();
  }

  @Override
  public MetadataCache getCache() {
    return rootContext.getCache();
  }

  @Override
  public Optional<RouterOutputMetadataContext> getRouterOutputMetadataContext() {
    return rootContext.getRouterOutputMetadataContext();
  }

  @Override
  public Optional<ScopeOutputMetadataContext> getScopeOutputMetadataContext() {
    return rootContext.getScopeOutputMetadataContext();
  }
}
