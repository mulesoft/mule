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
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.sdk.api.metadata.NullMetadataResolver;

/**
 * Adapter of {@link org.mule.sdk.api.metadata.resolving.OutputTypeResolver} to {@link OutputTypeResolver}
 *
 * @since 4.5.0
 */
public class MuleOutputTypeResolverAdapter implements OutputTypeResolver {

  private final org.mule.sdk.api.metadata.resolving.OutputTypeResolver delegate;

  MuleOutputTypeResolverAdapter(org.mule.sdk.api.metadata.resolving.OutputTypeResolver delegate) {
    this.delegate = delegate;
  }

  public static OutputTypeResolver from(Object resolver) {
    checkArgument(resolver != null, "Cannot adapt null resolver");

    if (resolver instanceof OutputTypeResolver) {
      return (OutputTypeResolver) resolver;
    } else if (resolver instanceof NullMetadataResolver) {
      return new org.mule.runtime.extension.api.metadata.NullMetadataResolver();
    } else if (resolver instanceof SdkOutputTypeResolverAdapter) {
      return ((SdkOutputTypeResolverAdapter) resolver).getDelegate();
    } else if (resolver instanceof org.mule.sdk.api.metadata.resolving.OutputTypeResolver) {
      return new MuleOutputTypeResolverAdapter((org.mule.sdk.api.metadata.resolving.OutputTypeResolver) resolver);
    } else {
      throw new IllegalArgumentException(format("Resolver of class '%s' is neither a '%s' nor a '%s'",
                                                resolver.getClass().getName(),
                                                OutputTypeResolver.class.getName(),
                                                org.mule.sdk.api.metadata.resolving.OutputTypeResolver.class.getName()));
    }
  }

  @Override
  public String getCategoryName() {
    return delegate.getCategoryName();
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key) throws MetadataResolvingException, ConnectionException {
    return delegate.getOutputType(new SdkMetadataContextAdapter(context), key);
  }
}
