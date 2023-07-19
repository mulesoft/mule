/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
