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
import org.mule.sdk.api.metadata.MetadataContext;
import org.mule.sdk.api.metadata.resolving.InputTypeResolver;

public class SdkInputTypeResolverAdapter implements InputTypeResolver {

  private org.mule.runtime.api.metadata.resolving.InputTypeResolver delegate;

  public SdkInputTypeResolverAdapter(org.mule.runtime.api.metadata.resolving.InputTypeResolver delegate) {
    this.delegate = delegate;
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, Object key)
      throws MetadataResolvingException, ConnectionException {
    return delegate.getInputMetadata(new MuleMetadataContextAdapter(context), key);
  }

  @Override
  public String getCategoryName() {
    return delegate.getCategoryName();
  }

  @Override
  public String getResolverName() {
    return delegate.getResolverName();
  }

  public org.mule.runtime.api.metadata.resolving.InputTypeResolver getDelegate() {
    return delegate;
  }
}
