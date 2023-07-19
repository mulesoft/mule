/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metadata.internal.cache.lazy;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.metadata.internal.cache.MetadataCacheManager;

import java.util.function.Supplier;

public class DelegateMetadataCacheManager implements MetadataCacheManager, Initialisable {

  private final Supplier<MetadataCacheManager> metadataCacheManagerSupplier;
  private MetadataCacheManager metadataCacheManagerDelegate;

  public DelegateMetadataCacheManager(Supplier<MetadataCacheManager> metadataCacheManagerSupplier) {
    this.metadataCacheManagerSupplier = metadataCacheManagerSupplier;
  }

  @Override
  public void initialise() throws InitialisationException {
    this.metadataCacheManagerDelegate = metadataCacheManagerSupplier.get();
  }

  @Override
  public MetadataCache getOrCreateCache(String id) {
    return metadataCacheManagerDelegate.getOrCreateCache(id);
  }

  @Override
  public void updateCache(String id, MetadataCache cache) {
    metadataCacheManagerDelegate.updateCache(id, cache);
  }

  @Override
  public void dispose(String id) {
    metadataCacheManagerDelegate.dispose(id);
  }
}
