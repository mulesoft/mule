/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.util.store;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.store.PartitionableExpirableObjectStore;
import org.mule.runtime.core.api.store.PartitionableObjectStore;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;

public class MuleObjectStoreManager implements ObjectStoreManager, MuleContextAware, Initialisable, Disposable {

  private static Logger logger = getLogger(MuleObjectStoreManager.class);

  protected Scheduler scheduler;
  MuleContext muleContext;
  protected ConcurrentMap<String, ObjectStore<?>> stores = new ConcurrentHashMap<String, ObjectStore<?>>();
  private final ConcurrentMap<String, ScheduledFuture<?>> monitors = new ConcurrentHashMap<>();
  private String baseTransientStoreKey = OBJECT_STORE_DEFAULT_IN_MEMORY_NAME;
  private String basePersistentStoreKey = OBJECT_STORE_DEFAULT_PERSISTENT_NAME;
  private String baseTransientUserStoreKey = DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME;
  private String basePersistentUserStoreKey = DEFAULT_USER_OBJECT_STORE_NAME;

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name) {
    return this.getObjectStore(name, false);
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name, boolean isPersistent) {
    return internalCreateStore(getBaseStore(isPersistent), name, UNBOUNDED, UNBOUNDED, 0);
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name, boolean isPersistent, int maxEntries,
                                                                          long entryTTL, long expirationInterval) {
    return internalCreateStore(getBaseStore(isPersistent), name, maxEntries, entryTTL, expirationInterval);
  }

  public <T extends ObjectStore<? extends Serializable>> T getUserObjectStore(String name, boolean isPersistent) {
    return internalCreateStore(getBaseUserStore(isPersistent), name, UNBOUNDED, UNBOUNDED, 0);
  }

  public <T extends ObjectStore<? extends Serializable>> T getUserObjectStore(String name, boolean isPersistent, int maxEntries,
                                                                              long entryTTL, long expirationInterval) {
    return internalCreateStore(getBaseUserStore(isPersistent), name, maxEntries, entryTTL, expirationInterval);
  }

  @SuppressWarnings({"unchecked"})
  synchronized public <T extends ObjectStore<? extends Serializable>> T internalCreateStore(ListableObjectStore<? extends Serializable> baseStore,
                                                                                            String name, int maxEntries,
                                                                                            long entryTTL,
                                                                                            long expirationInterval) {

    if (maxEntries < UNBOUNDED) {
      maxEntries = UNBOUNDED;
    }

    if (entryTTL < UNBOUNDED) {
      entryTTL = UNBOUNDED;
    }

    if (stores.containsKey(name)) {
      return (T) stores.get(name);
    }

    T store = null;
    try {
      store = this.getPartitionFromBaseObjectStore(baseStore, name);
    } catch (ObjectStoreException e) {
      // TODO In order to avoid breaking backward compatibility. In the future
      // this method must throw object store creation exception
      throw new MuleRuntimeException(e);
    }
    if (maxEntries == UNBOUNDED && entryTTL == UNBOUNDED) {
      return putInStoreMap(name, store);
    } else {
      return getMonitorablePartition(name, baseStore, store, entryTTL, maxEntries, expirationInterval);
    }
  }

  private <T extends ListableObjectStore<? extends Serializable>> T getBaseUserStore(boolean persistent) {
    T baseStore;
    if (persistent) {
      baseStore = muleContext.getRegistry().lookupObject(this.basePersistentUserStoreKey);
    } else {
      baseStore = muleContext.getRegistry().lookupObject(this.baseTransientUserStoreKey);
    }
    return baseStore;
  }

  private <T extends ListableObjectStore<? extends Serializable>> T getBaseStore(boolean persistent) {
    T baseStore;
    if (persistent) {
      baseStore = muleContext.getRegistry().lookupObject(this.basePersistentStoreKey);
    } else {
      baseStore = muleContext.getRegistry().lookupObject(this.baseTransientStoreKey);
    }
    return baseStore;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private <T extends ObjectStore<? extends Serializable>> T getPartitionFromBaseObjectStore(ListableObjectStore<? extends Serializable> baseStore,
                                                                                            String partitionName)
      throws ObjectStoreException {
    if (baseStore instanceof PartitionableObjectStore) {
      ObjectStorePartition objectStorePartition = new ObjectStorePartition(partitionName, (PartitionableObjectStore) baseStore);
      objectStorePartition.open();
      return (T) objectStorePartition;
    } else {
      PartitionedObjectStoreWrapper partitionedObjectStoreWrapper =
          new PartitionedObjectStoreWrapper(partitionName, muleContext, baseStore);
      partitionedObjectStoreWrapper.open();
      return (T) partitionedObjectStoreWrapper;
    }
  }

  private <T extends ObjectStore<? extends Serializable>> T putInStoreMap(String name, T store) {
    @SuppressWarnings("unchecked")
    T previous = (T) stores.putIfAbsent(name, store);
    if (previous == null) {
      return store;
    } else {
      return previous;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private <T extends ObjectStore<? extends Serializable>> T getMonitorablePartition(String name, ListableObjectStore baseStore,
                                                                                    T store, long entryTTL, int maxEntries,
                                                                                    long expirationInterval) {
    if (baseStore instanceof PartitionableExpirableObjectStore) {
      T previous = (T) stores.putIfAbsent(name, store);
      if (previous == null) {
        ScheduledFuture<?> future = scheduler
            .scheduleWithFixedDelay(new Monitor(name, (PartitionableExpirableObjectStore) baseStore, entryTTL, maxEntries), 0,
                                    expirationInterval, MILLISECONDS);
        monitors.put(name, future);
        return store;
      } else {
        return previous;
      }
    } else {
      MonitoredObjectStoreWrapper monObjectStore;
      // Using synchronization here in order to avoid initialising the
      // monitored object store wrapper for nothing and having to dispose
      // or putting an uninitialised ObjectStore
      synchronized (this) {
        if (stores.containsKey(name)) {
          return (T) stores.get(name);
        }
        monObjectStore = new MonitoredObjectStoreWrapper((ListableObjectStore) store, maxEntries, entryTTL, expirationInterval);
        monObjectStore.setMuleContext(muleContext);
        try {
          monObjectStore.initialise();
        } catch (InitialisationException e) {
          throw new MuleRuntimeException(e);
        }
        stores.put(name, monObjectStore);
      }
      return (T) monObjectStore;
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public void clearStoreCache() {
    stores.clear();
  }

  @Override
  public void dispose() {
    scheduler.stop();
    for (ObjectStore<?> objectStore : stores.values()) {
      if (objectStore instanceof Disposable) {
        ((Disposable) objectStore).dispose();
      }
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    scheduler = muleContext.getSchedulerService()
        .customScheduler(muleContext.getSchedulerBaseConfig().withName("ObjectStoreManager-Monitor").withMaxConcurrentTasks(1));
  }

  @Override
  public void disposeStore(ObjectStore<? extends Serializable> store) throws ObjectStoreException {
    if (store instanceof ObjectStorePartition) {
      ObjectStorePartition partition = (ObjectStorePartition) store;
      String partitionName = partition.getPartitionName();
      partition.getBaseStore().disposePartition(partitionName);

      ScheduledFuture<?> future = monitors.remove(partitionName);
      if (future != null) {
        future.cancel(false);
      }
      stores.remove(partitionName);
    } else {
      try {
        store.clear();
      } catch (UnsupportedOperationException e) {
        logger.warn(String.format("ObjectStore of class %s does not support clearing", store.getClass().getCanonicalName()), e);
      }
      try {
        stores.values().remove(store);
      } catch (Exception e) {
        logger.warn("Can not remove object store" + store.toString(), e);
      }
    }

    if (store instanceof Disposable) {
      ((Disposable) store).dispose();
    }
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
          logger.warn("Running expirty on partition " + partitionName + " of " + store + " threw " + e + ":" + e.getMessage());
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

  public void setBasePersistentUserStoreKey(String basePersistentUserStoreKey) {
    this.basePersistentUserStoreKey = basePersistentUserStoreKey;
  }

  public void setBaseTransientUserStoreKey(String baseTransientUserStoreKey) {
    this.baseTransientUserStoreKey = baseTransientUserStoreKey;
  }
}
