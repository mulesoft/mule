/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataProperty;

import java.util.Optional;
import java.util.Set;

public class MuleMetadataKeyAdapter implements MetadataKey {

  private final org.mule.sdk.api.metadata.MetadataKey delegate;

  public MuleMetadataKeyAdapter(org.mule.sdk.api.metadata.MetadataKey delegate) {
    this.delegate = delegate;
  }

  @Override
  public <T extends MetadataProperty> Optional<T> getMetadataProperty(Class<T> propertyType) {
    return Optional.empty();
  }

  @Override
  public Set<MetadataProperty> getProperties() {
    return null;
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public String getDisplayName() {
    return delegate.getDisplayName();
  }

  @Override
  public Set<MetadataKey> getChilds() {
    return null;
  }

  @Override
  public String getPartName() {
    return delegate.getPartName();
  }
}
