/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata.cache;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.api.util.LazyValue;

import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * //TODO
 */
public class DefaultMetadataCacheManager implements MetadataCacheManager, Startable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMetadataCacheManager.class);
  public static final String PERSISTENT_METADATA_SERVICE_CACHE = "_mulePersistentMetadataService";

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  private ObjectStoreManager objectStoreManager;

  @Inject
  private LockFactory lockFactory;

  private LazyValue<ObjectStore<MetadataCache>> metadataStore;

  @Override
  public void start() {
    metadataStore = new LazyValue<>(() -> objectStoreManager.getOrCreateObjectStore(PERSISTENT_METADATA_SERVICE_CACHE,
                                                                                    ObjectStoreSettings.builder()
                                                                                        .persistent(true)
                                                                                        .build()));
  }

  @Override
  public MetadataCache getCache(String id) {
    return withKeyLock(id, key -> {
      try {
        if (metadataStore.get().contains(key)) {
          return metadataStore.get().retrieve(key);
        }

        LOGGER.debug("Creating new cache " + id);
        DefaultMetadataCache metadataCache = new DefaultMetadataCache();
        metadataStore.get().store(key, metadataCache);
        return metadataCache;

      } catch (Exception e) {
        String msg = String.format("An error occurred while retrieving the MetadataCache with ID '%s': %s",
                                   id, e.getMessage());
        LOGGER.error(msg);
        throw new RuntimeException(msg, e);
      }
    });
  }

  @Override
  public void saveCache(String id, MetadataCache cache) {
    withKeyLock(id, key -> {
      try {
        LOGGER.debug("saveCache Key: " + id);
        if (metadataStore.get().contains(key)) {
          metadataStore.get().remove(key);
        }
        metadataStore.get().store(key, cache);
      } catch (Exception e) {
        String msg = String.format("An error occurred while updating the MetadataCache with ID '%s': %s",
                                   id, e.getMessage());
        LOGGER.error(msg);
        throw new RuntimeException(msg, e);
      }
      return null;
    });
  }

  @Override
  public void dispose(String keyHash) {
    withKeyLock(keyHash, key -> {
      try {
        metadataStore.get().remove(key);
      } catch (ObjectDoesNotExistException e) {
        LOGGER.debug("No exact match found: " + key);
        disposeAllMatches(keyHash);
      } catch (Exception e) {
        String msg = String.format("An error occurred while disposing the MetadataCache with ID '%s': %s",
                                   keyHash, e.getMessage());
        LOGGER.error(msg);
        throw new RuntimeException(msg, e);
      }
      return null;
    });
  }

  private void disposeAllMatches(String keyHash) {
    try {
      metadataStore.get().allKeys().stream()
          .filter(id -> id.startsWith(keyHash))
          .forEach(id -> {
            try {
              this.dispose(id);
            } catch (Exception inner) {
              LOGGER.debug(String.format("Failed to dispose ID '%s' with partial match: %s", id, inner.getMessage()));
            }
          });
    } catch (ObjectStoreException e) {
      String msg = String.format("Failed to perform a cache disposal for partial ID '%s': %s",
                                 keyHash, e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg, e);
    }
  }

  private <T> T withKeyLock(String key, Function<String, T> producer) {
    Lock lock = lockFactory.createLock(key);
    lock.lock();
    try {
      return producer.apply(key);
    } finally {
      lock.unlock();
    }
  }

}
