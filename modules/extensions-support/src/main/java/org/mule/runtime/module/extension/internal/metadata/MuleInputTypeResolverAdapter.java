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
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.sdk.api.metadata.NullMetadataResolver;

public class MuleInputTypeResolverAdapter implements InputTypeResolver {

  private final org.mule.sdk.api.metadata.resolving.InputTypeResolver delegate;

  MuleInputTypeResolverAdapter(org.mule.sdk.api.metadata.resolving.InputTypeResolver delegate) {
    this.delegate = delegate;
  }

  public static InputTypeResolver from(Object resolver) {
    checkArgument(resolver != null, "Cannot adapt null resolver");

    if (resolver instanceof InputTypeResolver) {
      return (InputTypeResolver) resolver;
    } else if (resolver instanceof NullMetadataResolver) {
      return new org.mule.runtime.extension.api.metadata.NullMetadataResolver();
    } else if (resolver instanceof org.mule.sdk.api.metadata.resolving.InputTypeResolver) {
      return new MuleInputTypeResolverAdapter((org.mule.sdk.api.metadata.resolving.InputTypeResolver) resolver);
    } else {
      throw new IllegalArgumentException(format("Resolver of class '%s' is neither a '%s' nor a '%s'",
                                                resolver.getClass().getName(),
                                                InputTypeResolver.class.getName(),
                                                org.mule.sdk.api.metadata.resolving.InputTypeResolver.class.getName()));
    }
  }

  @Override
  public String getResolverName() {
    return delegate.getResolverName();
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, Object key)
      throws MetadataResolvingException, ConnectionException {
    return null;
  }

  @Override
  public String getCategoryName() {
    return delegate.getCategoryName();
  }
}
