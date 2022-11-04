/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache;

import org.mule.runtime.core.internal.util.cache.CacheIdBuilderAdapter;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;

import java.util.ArrayList;
import java.util.List;

public class MetadataCacheIdBuilderAdapter implements CacheIdBuilderAdapter<MetadataCacheId> {

  private String name;
  private int value;
  private final List<MetadataCacheId> parts = new ArrayList<>();

  @Override
  public CacheIdBuilderAdapter<MetadataCacheId> withSourceElementName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public CacheIdBuilderAdapter<MetadataCacheId> withHashValue(int value) {
    this.value = value;
    return this;
  }

  @Override
  public CacheIdBuilderAdapter<MetadataCacheId> containing(List<MetadataCacheId> parts) {
    this.parts.addAll(parts);
    return this;
  }

  @Override
  public MetadataCacheId build() {
    if (parts.isEmpty()) {
      return new MetadataCacheId(value, name);
    }
    return new MetadataCacheId(parts, name);
  }
}
