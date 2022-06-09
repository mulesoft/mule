/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.util.Set;
import java.util.stream.Collectors;

public class MuleTypeKeysResolverAdapter implements TypeKeysResolver {

  private final org.mule.sdk.api.metadata.resolving.TypeKeysResolver delegate;

  public MuleTypeKeysResolverAdapter(org.mule.sdk.api.metadata.resolving.TypeKeysResolver delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getCategoryName() {
    return delegate.getCategoryName();
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return delegate.getKeys(new SdkMetadataContextAdapter(context))
        .stream().map(MuleMetadataKeyAdapter::new).collect(toSet());
  }

  @Override
  public String getResolverName() {
    return delegate.getResolverName();
  }
}
