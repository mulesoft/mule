/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static java.util.Comparator.comparing;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.privileged.store.DeserializationPostInitialisable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ScheduledFuture;

/**
 * The MonitoredObjectStoreWrapper wraps an ObjectStore which does not support direct expiry and adds this behavior
 */
public class MonitoredObjectStoreWrapper<T extends Serializable> extends TemplateObjectStore<T>
    implements Runnable, MuleContextAware, Initialisable, Disposable {

  private static Logger logger = LoggerFactory.getLogger(MonitoredObjectStoreWrapper.class);

  protected MuleContext context;
  private Scheduler scheduler;
  private ScheduledFuture<?> scheduledTask;
  ObjectStore<StoredObject<T>> baseStore;

  /**
   * the maximum number of entries that this store keeps around. Specify <em>-1</em> if the store is supposed to be "unbounded".
   */
  private Integer maxEntries = null;

  /**
   * The time-to-live for each message ID, specified in milliseconds, or <em>-1</em> for entries that should never expire. <b>DO
   * NOT</b> combine this with an unbounded store!
   */
  private Long entryTtl = null;

  /**
   * The interval for periodic bounded size enforcement and entry expiration, specified in milliseconds. Arbitrary positive values
   * between 1 millisecond and several hours or days are possible, but should be chosen carefully according to the expected
   * message rate to prevent out of memory conditions.
   */
  private long expirationInterval = 1000;

  /**
   * A name for this store, can be used for logging and identification purposes.
   */
  protected String name = null;

  public MonitoredObjectStoreWrapper(ObjectStore<StoredObject<T>> baseStore, ObjectStoreSettings settings) {
    this.baseStore = baseStore;
    maxEntries = settings.getMaxEntries().orElse(null);
    entryTtl = settings.getEntryTTL().orElse(null);
    expirationInterval = settings.getExpirationInterval();
  }

  @Override
  protected boolean doContains(String key) throws ObjectStoreException {
    return getStore().contains(key);
  }

  @Override
  protected void doStore(String key, T value) throws ObjectStoreException {
    Long time = Long.valueOf(System.currentTimeMillis());
    getStore().store(key, new StoredObject<>(value, time, key));
  }

  @Override
  protected T doRetrieve(String key) throws ObjectStoreException {
    return getStore().retrieve(key).getItem();
  }

  @Override
  public Map<String, T> retrieveAll() throws ObjectStoreException {
    return getStore().retrieveAll().values().stream().collect(toMap(StoredObject::getKey, StoredObject::getItem));
  }

  @Override
  public void clear() throws ObjectStoreException {
    this.getStore().clear();
  }

  @Override
  protected T doRemove(String key) throws ObjectStoreException {
    StoredObject<T> object = getStore().remove(key);
    if (object == null) {
      return null;
    } else {
      return object.getItem();
    }
  }

  @Override
  public boolean isPersistent() {
    return getStore().isPersistent();
  }

  @Override
  public void open() throws ObjectStoreException {
    getStore().open();
  }

  @Override
  public void close() throws ObjectStoreException {
    getStore().close();
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    return getStore().allKeys();
  }

  private ObjectStore<StoredObject<T>> getStore() {
    if (baseStore == null) {
      baseStore = ((MuleContextWithRegistries) context).getRegistry().lookupObject(BASE_PERSISTENT_OBJECT_STORE_KEY);
    }
    return baseStore;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.context = context;
  }

  @Override
  public void run() {
    if (context.isPrimaryPollingInstance()) {
      expire();
    }
  }

  public void expire() {
    try {
      final long now = System.currentTimeMillis();
      List<String> keys = allKeys();
      int excess = (allKeys().size() - maxEntries);

      PriorityQueue<StoredObject<T>> sortedMaxEntries = null;

      if (excess > 0) {
        sortedMaxEntries = new PriorityQueue<>(excess, comparing(paramT -> paramT.timestamp));
      }

      ObjectStore<StoredObject<T>> store = getStore();
      for (String key : keys) {
        StoredObject<T> obj = store.retrieve(key);

        if (entryTtl != null && now - obj.getTimestamp() >= entryTtl) {
          remove(key);
          excess--;
        } else if (maxEntries != null && excess > 0) {
          sortedMaxEntries.offer(obj);
        }
      }

      if (sortedMaxEntries != null) {
        StoredObject<T> obj = sortedMaxEntries.poll();
        while (obj != null && excess > 0) {
          remove(obj.getKey());
          excess--;
          obj = sortedMaxEntries.poll();
        }
      }
    } catch (Exception e) {
      logger.warn("Running expiry on " + baseStore + " threw " + e + ":" + e.getMessage(), e);
    }
  }

  @Override
  public void dispose() {
    if (scheduledTask != null) {
      scheduledTask.cancel(true);
      scheduler.stop();
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    if (name == null) {
      name = UUID.getUUID();
    }

    if (expirationInterval <= 0) {
      throw new IllegalArgumentException(CoreMessages
          .propertyHasInvalidValue("expirationInterval", new Long(expirationInterval)).toString());
    }

    if (scheduler == null) {
      this.scheduler = context.getSchedulerService()
          .customScheduler(context.getSchedulerBaseConfig().withName(name + "-Monitor").withMaxConcurrentTasks(1));
      scheduledTask = scheduler.scheduleWithFixedDelay(this, 0, expirationInterval, MILLISECONDS);
    }
  }

  public static class StoredObject<T> implements Serializable, DeserializationPostInitialisable {

    private static final long serialVersionUID = 8656763235928199259L;
    final private T item;
    final private Long timestamp;
    final private String key;

    public StoredObject(T item, Long timestamp, String key) {
      super();
      this.item = item;
      this.timestamp = timestamp;
      this.key = key;
    }

    public T getItem() {
      return item;
    }

    public Long getTimestamp() {
      return timestamp;
    }

    public String getKey() {
      return key;
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link DeserializationPostInitialisable} is used. This will get invoked after the object
     * has been deserialized passing in the current MuleContext.
     *
     * @param muleContext the current muleContext instance
     * @throws MuleException if there is an error initializing
     */
    @SuppressWarnings({"unused"})
    private void initAfterDeserialisation(MuleContext muleContext) throws MuleException {
      if (item instanceof DeserializationPostInitialisable) {
        try {
          DeserializationPostInitialisable.Implementation.init(item, muleContext);
        } catch (Exception e) {
          throw new DefaultMuleException(e);
        }
      }
    }
  }

}
