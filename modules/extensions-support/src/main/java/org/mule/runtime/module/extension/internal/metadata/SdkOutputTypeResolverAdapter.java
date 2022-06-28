/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.sdk.api.metadata.MetadataContext;
import org.mule.sdk.api.metadata.resolving.OutputTypeResolver;

public class SdkOutputTypeResolverAdapter implements OutputTypeResolver {

  private final org.mule.runtime.api.metadata.resolving.OutputTypeResolver delegate;

  public SdkOutputTypeResolverAdapter(org.mule.runtime.api.metadata.resolving.OutputTypeResolver delegate) {
    this.delegate = delegate;
  }

  public static OutputTypeResolver from(org.mule.runtime.api.metadata.resolving.OutputTypeResolver delegate) {
    if (delegate instanceof NullMetadataResolver) {
      return new org.mule.sdk.api.metadata.NullMetadataResolver();
    } else {
      return new SdkOutputTypeResolverAdapter(delegate);
    }
  }

  public org.mule.runtime.api.metadata.resolving.OutputTypeResolver getDelegate() {
    return delegate;
  }

  @Override
  public String getCategoryName() {
    return delegate.getCategoryName();
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key) throws MetadataResolvingException, ConnectionException {
    return delegate.getOutputType(new MuleMetadataContextAdapter(context), key);
  }

  @Override
  public String getResolverName() {
    return delegate.getResolverName();
  }

}
