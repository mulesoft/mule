/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.sdk.api.metadata.NullMetadataResolver;

import java.util.HashSet;
import java.util.Set;

/**
 * Adapts a {@link org.mule.sdk.api.metadata.resolving.TypeKeysResolver} into a {@link TypeKeysResolver}
 *
 * @since 4.5.0
 */
public class MuleTypeKeysResolverAdapter implements TypeKeysResolver, MuleMetadataTypeResolverAdapter {

  private final org.mule.sdk.api.metadata.resolving.TypeKeysResolver delegate;

  MuleTypeKeysResolverAdapter(org.mule.sdk.api.metadata.resolving.TypeKeysResolver delegate) {
    this.delegate = delegate;
  }

  public static TypeKeysResolver from(Object resolver) {
    checkArgument(resolver != null, "Cannot adapt null resolver");

    if (resolver instanceof TypeKeysResolver) {
      return (TypeKeysResolver) resolver;
    } else if (resolver instanceof NullMetadataResolver) {
      return new org.mule.runtime.extension.api.metadata.NullMetadataResolver();
    } else if (resolver instanceof org.mule.sdk.api.metadata.resolving.TypeKeysResolver) {
      return new MuleTypeKeysResolverAdapter((org.mule.sdk.api.metadata.resolving.TypeKeysResolver) resolver);
    } else {
      throw new IllegalArgumentException(format("Resolver of class '%s' is neither a '%s' nor a '%s'",
                                                resolver.getClass().getName(),
                                                TypeKeysResolver.class.getName(),
                                                org.mule.sdk.api.metadata.resolving.TypeKeysResolver.class.getName()));
    }
  }

  @Override
  public String getCategoryName() {
    return delegate.getCategoryName();
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    Set<MetadataKey> metadataKeys = new HashSet<>();
    delegate.getKeys(new SdkMetadataContextAdapter(context))
        .forEach(metadataKey -> metadataKeys.add(new MuleMetadataKeyAdapter(metadataKey)));

    return metadataKeys;
  }

  @Override
  public String getResolverName() {
    return delegate.getResolverName();
  }

  @Override
  public Class<?> getDelegateResolverClass() {
    return delegate.getClass();
  }
}
