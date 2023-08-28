/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metadata.internal;

import org.mule.runtime.extension.api.metadata.NullMetadataResolver;

import java.util.function.Supplier;

/**
 * {@link Supplier} implementation which returns a {@link NullMetadataResolver} instance. This supplier always returns the same
 * instance.
 *
 * @since 4.0
 */
public class NullMetadataResolverSupplier implements Supplier<NullMetadataResolver> {

  private static final NullMetadataResolver resolver = new NullMetadataResolver();

  @Override
  public NullMetadataResolver get() {
    return resolver;
  }
}
