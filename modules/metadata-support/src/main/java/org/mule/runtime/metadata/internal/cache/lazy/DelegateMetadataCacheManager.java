/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache.lazy;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.metadata.internal.cache.MetadataCacheManager;

import java.util.function.Function;

import jakarta.inject.Inject;

public class DelegateMetadataCacheManager implements MetadataCacheManager, Initialisable {

  private final Function<Registry, MetadataCacheManager> metadataCacheManagerSupplier;

  @Inject
  private Registry registry;

  private MetadataCacheManager metadataCacheManagerDelegate;

  public DelegateMetadataCacheManager(Function<Registry, MetadataCacheManager> metadataCacheManagerSupplier) {
    this.metadataCacheManagerSupplier = metadataCacheManagerSupplier;
  }

  @Override
  public void initialise() throws InitialisationException {
    this.metadataCacheManagerDelegate = metadataCacheManagerSupplier.apply(registry);
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
