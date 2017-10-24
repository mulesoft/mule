/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util.store;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.api.store.PartitionableObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.store.PartitionableExpirableObjectStore;

import org.slf4j.Logger;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import javax.inject.Inject;

public class MuleObjectStoreManager implements ObjectStoreManager, Initialisable, Disposable {

  private static Logger LOGGER = getLogger(MuleObjectStoreManager.class);
  public static final int UNBOUNDED = 0;

  private SchedulerService schedulerService;
  private Registry registry;
  private MuleContext muleContext;

  private final ConcurrentMap<String, ObjectStore<?>> stores = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, ScheduledFuture<?>> monitors = new ConcurrentHashMap<>();

  private String baseTransientStoreKey = BASE_IN_MEMORY_OBJECT_STORE_KEY;
  private String basePersistentStoreKey = BASE_PERSISTENT_OBJECT_STORE_KEY;

  private ObjectStore<?> baseTransientStore;
  private ObjectStore<?> basePersistentStore;
  private ObjectStore<?> baseTransientPartition;
  private ObjectStore<?> basePersistentPartition;
  private Scheduler scheduler;

  @Override
  public void initialise() throws InitialisationException {
    basePersistentStore = lookupBaseStore(basePersistentStoreKey, "Persistent");
    baseTransientStore = lookupBaseStore(baseTransientStoreKey, "Transient");

    try {
      baseTransientPartition = getPartitionFromBaseObjectStore(baseTransientStore, baseTransientStoreKey);
      basePersistentPartition = getPartitionFromBaseObjectStore(basePersistentStore, basePersistentStoreKey);
    } catch (ObjectStoreException e) {
      throw new InitialisationException(e, this);
    }

    scheduler = schedulerService
        .customScheduler(muleContext.getSchedulerBaseConfig().withName("ObjectStoreManager-Monitor").withMaxConcurrentTasks(1));
  }

  private ObjectStore<?> lookupBaseStore(String key, String baseType) throws InitialisationException {
    return registry.<ObjectStore>lookupByName(key)
        .orElseThrow(() -> new InitialisationException(createStaticMessage(format("%s base store of key '%s' does not exists",
                                                                                  baseType, key)),
                                                       this));
  }

  @Override
  public void dispose() {
    if (scheduler != null) {
      scheduler.stop();
    }

    basePersistentPartition = null;
    baseTransientPartition = null;
    basePersistentStore = null;
    baseTransientStore = null;

    stores.values().forEach(store -> disposeIfNeeded(store, LOGGER));
    stores.clear();
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name) {
    if (basePersistentStoreKey.equals(name)) {
      return (T) basePersistentPartition;
    }

    if (baseTransientStoreKey.equals(name)) {
      return (T) baseTransientPartition;
    }

    T store;
    synchronized (stores) {
      store = (T) stores.get(name);
    }

    if (store == null) {
      throw noSuchStoreException(name);
    }

    return store;
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T createObjectStore(String name, ObjectStoreSettings settings) {
    synchronized (stores) {
      if (baseTransientStoreKey.equals(name) ||
          basePersistentStoreKey.equals(name) ||
          stores.containsKey(name)) {
        throw new IllegalArgumentException("An Object Store was already defined for name " + name);
      }

      T store = doCreateObjectStore(name, settings);
      stores.put(name, store);

      return store;
    }
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getOrCreateObjectStore(String name, ObjectStoreSettings settings) {
    T objectStore;

    synchronized (stores) {
      try {
        objectStore = getObjectStore(name);
      } catch (NoSuchElementException e) {
        objectStore = createObjectStore(name, settings);
      }
    }

    return objectStore;
  }

  private <T extends ObjectStore<?>> T doCreateObjectStore(String name, ObjectStoreSettings settings) {
    final ObjectStore<? extends Serializable> baseStore = getBaseStore(settings);
    T store;
    try {
      store = getPartitionFromBaseObjectStore(baseStore, name);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Found exception trying to create Object Store of name " + name), e);
    }

    if (settings.getExpirationInterval() > 0 &&
        (settings.getMaxEntries().isPresent() || settings.getEntryTTL().isPresent())) {
      store = getMonitorablePartition(name, baseStore, store, settings);
    }

    return store;
  }

  private <T extends ObjectStore<? extends Serializable>> T getBaseStore(ObjectStoreSettings settings) {
    return settings.isPersistent() ? (T) basePersistentStore : (T) baseTransientStore;
  }

  private <T extends ObjectStore<? extends Serializable>> T getPartitionFromBaseObjectStore(
                                                                                            ObjectStore<? extends Serializable> baseStore,
                                                                                            String partitionName)
      throws ObjectStoreException {

    if (baseStore instanceof PartitionableObjectStore) {
      ObjectStorePartition objectStorePartition = new ObjectStorePartition(partitionName, (PartitionableObjectStore) baseStore);
      objectStorePartition.open();
      return (T) objectStorePartition;
    } else {
      PartitionedObjectStoreWrapper partitionedObjectStoreWrapper = new PartitionedObjectStoreWrapper(partitionName, baseStore);
      partitionedObjectStoreWrapper.open();
      return (T) partitionedObjectStoreWrapper;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private <T extends ObjectStore<? extends Serializable>> T getMonitorablePartition(String name,
                                                                                    ObjectStore baseStore,
                                                                                    T store,
                                                                                    ObjectStoreSettings settings) {
    if (baseStore instanceof PartitionableExpirableObjectStore) {
      ScheduledFuture<?> future = scheduler
          .scheduleWithFixedDelay(new Monitor(name,
                                              (PartitionableExpirableObjectStore) baseStore,
                                              settings.getEntryTTL().orElse(0L),
                                              settings.getMaxEntries().orElse(UNBOUNDED)),
                                  0,
                                  settings.getExpirationInterval(), MILLISECONDS);
      monitors.put(name, future);
      return store;
    } else {
      MonitoredObjectStoreWrapper monObjectStore;
      // Using synchronization here in order to avoid initialising the
      // monitored object store wrapper for nothing and having to dispose
      // or putting an uninitialised ObjectStore
      synchronized (this) {
        monObjectStore = new MonitoredObjectStoreWrapper(store, settings);
        monObjectStore.setMuleContext(muleContext);
        try {
          monObjectStore.initialise();
        } catch (InitialisationException e) {
          throw new MuleRuntimeException(e);
        }
      }
      return (T) monObjectStore;
    }
  }

  public void clearStoreCache() {
    stores.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disposeStore(String name) throws ObjectStoreException {
    if (basePersistentStoreKey.equals(name) || baseTransientStoreKey.equals(name)) {
      return;
    }

    ObjectStore store = stores.remove(name);
    if (store == null) {
      throw noSuchStoreException(name);
    }

    try {
      if (store instanceof ObjectStorePartition) {
        ObjectStorePartition partition = (ObjectStorePartition) store;
        String partitionName = partition.getPartitionName();
        partition.getBaseStore().disposePartition(partitionName);

        ScheduledFuture<?> future = monitors.remove(partitionName);
        if (future != null) {
          future.cancel(false);
        }
      } else {
        try {
          store.clear();
        } catch (UnsupportedOperationException e) {
          LOGGER.warn(format("ObjectStore of class %s does not support clearing", store.getClass().getCanonicalName()), e);
        }
      }
    } finally {
      disposeIfNeeded(store, LOGGER);
    }
  }

  private NoSuchElementException noSuchStoreException(String name) {
    return new NoSuchElementException("ObjectStore '" + name + "' is not defined");
  }

  class Monitor implements Runnable {

    private final String partitionName;
    private final PartitionableExpirableObjectStore<? extends Serializable> store;
    private final long entryTTL;
    private final int maxEntries;

    public Monitor(String partitionName, PartitionableExpirableObjectStore<? extends Serializable> store, long entryTTL,
                   int maxEntries) {
      this.partitionName = partitionName;
      this.store = store;
      this.entryTTL = entryTTL;
      this.maxEntries = maxEntries;
    }

    @Override
    public void run() {
      if (muleContext.isPrimaryPollingInstance()) {
        try {
          store.expire(entryTTL, maxEntries, partitionName);
        } catch (Exception e) {
          LOGGER.warn("Running expirty on partition " + partitionName + " of " + store + " threw " + e + ":" + e.getMessage());
        }
      }
    }

  }

  int getMonitorsCount() {
    return monitors.size();
  }

  public void setBasePersistentStoreKey(String basePersistentStoreKey) {
    this.basePersistentStoreKey = basePersistentStoreKey;
  }

  public void setBaseTransientStoreKey(String baseTransientStoreKey) {
    this.baseTransientStoreKey = baseTransientStoreKey;
  }

  @Inject
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  @Inject
  public void setRegistry(Registry registry) {
    this.registry = registry;
  }

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
