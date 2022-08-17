/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.sdk.api.metadata.NullMetadataResolver;

public class MuleAttributesTypeResolverAdapter implements AttributesTypeResolver {

  private final org.mule.sdk.api.metadata.resolving.AttributesTypeResolver delegate;

  MuleAttributesTypeResolverAdapter(org.mule.sdk.api.metadata.resolving.AttributesTypeResolver delegate) {
    this.delegate = delegate;
  }

  public static AttributesTypeResolver from(Object resolver) {
    checkArgument(resolver != null, "Cannot adapt null resolver");

    if (resolver instanceof AttributesTypeResolver) {
      return (AttributesTypeResolver) resolver;
    } else if (resolver instanceof NullMetadataResolver) {
      return new org.mule.runtime.extension.api.metadata.NullMetadataResolver();
    } else if (resolver instanceof org.mule.sdk.api.metadata.resolving.AttributesTypeResolver) {
      return new MuleAttributesTypeResolverAdapter((org.mule.sdk.api.metadata.resolving.AttributesTypeResolver) resolver);
    } else {
      throw new IllegalArgumentException(format("Resolver of class '%s' is neither a '%s' nor a '%s'",
                                                resolver.getClass().getName(),
                                                AttributesTypeResolver.class.getName(),
                                                org.mule.sdk.api.metadata.resolving.AttributesTypeResolver.class.getName()));
    }
  }

  @Override
  public MetadataType getAttributesType(MetadataContext context, Object key)
      throws MetadataResolvingException, ConnectionException {
    return delegate.getAttributesType(new SdkMetadataContextAdapter(context), key);
  }

  @Override
  public String getCategoryName() {
    return delegate.getCategoryName();
  }

  @Override
  public String getResolverName() {
    return delegate.getResolverName();
  }

  public org.mule.sdk.api.metadata.resolving.AttributesTypeResolver getDelegate() {
    return delegate;
  }
}
