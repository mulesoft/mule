/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata.cache;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.metadata.MetadataCache;

/**
 * Manages the creation, updates and deletion of the {@link MetadataCache}s used for dynamic metadata resolution,
 * being the only way to obtain an instance of {@link MetadataCache}.
 *
 * @since 4.1.4, 4.2.0
 */
@NoImplement
public interface MetadataCacheManager {

  /**
   * Key under which the {@link MetadataCacheManager} can be found in the {@link org.mule.runtime.api.artifact.Registry}
   */
  String METADATA_CACHE_MANAGER_KEY = "_metadataCacheManager";

  /**
   * Returns the {@link MetadataCache} with the given {@code id} if one has already been defined. Otherwise,
   * a new instance of {@link MetadataCache} will be created, bound to the given {@code id} and then returned.
   *
   * @param id the cache identifier hash string
   * @return the {@link MetadataCache} bound to the given id, or a new cache of none existed.
   */
  MetadataCache getOrCreateCache(String id);

  /**
   * Replaces the old binding of the given {@code id} with the new {@code cache} element.
   *
   * @param id the cache identifier hash string
   * @param cache the new {@link MetadataCache} value bound to the given id
   */
  void updateCache(String id, MetadataCache cache);

  /**
   * If the given {@code id} references exactly one {@link MetadataCache} then that cache will be disposed.
   * Otherwise, all the caches that have an {@code id} starting with the given value will be disposed, based on the
   * pre-condition that id's are formed in a hierarchical way.
   *
   * @param id the given identifier hash string of the level at which the disposal should be performed
   */
  void dispose(String id);
}
