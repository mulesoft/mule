/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.store;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.store.ObjectStoreManager.UNBOUNDED;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.UUID;

import java.io.Serializable;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MonitoredObjectStoreWrapper wraps an ObjectStore which does not support direct expiry and adds this behavior
 */
public class MonitoredObjectStoreWrapper<T extends Serializable>
    implements ListableObjectStore<T>, Runnable, MuleContextAware, Initialisable, Disposable {

  private static Logger logger = LoggerFactory.getLogger(MonitoredObjectStoreWrapper.class);

  protected MuleContext context;
  private Scheduler scheduler;
  private ScheduledFuture<?> scheduledTask;
  ListableObjectStore<StoredObject<T>> baseStore;

  /**
   * the maximum number of entries that this store keeps around. Specify <em>-1</em> if the store is supposed to be "unbounded".
   */
  protected int maxEntries = 4000;

  /**
   * The time-to-live for each message ID, specified in milliseconds, or <em>-1</em> for entries that should never expire. <b>DO
   * NOT</b> combine this with an unbounded store!
   */
  protected long entryTTL = -1;

  /**
   * The interval for periodic bounded size enforcement and entry expiration, specified in milliseconds. Arbitrary positive values
   * between 1 millisecond and several hours or days are possible, but should be chosen carefully according to the expected
   * message rate to prevent out of memory conditions.
   */
  protected long expirationInterval = 1000;

  /**
   * A name for this store, can be used for logging and identification purposes.
   */
  protected String name = null;

  public MonitoredObjectStoreWrapper(ListableObjectStore<StoredObject<T>> baseStore) {
    this.baseStore = baseStore;
  }

  public MonitoredObjectStoreWrapper(ListableObjectStore<StoredObject<T>> baseStore, int maxEntries, long entryTTL,
                                     long expirationInterval) {
    this.baseStore = baseStore;
    this.maxEntries = maxEntries;
    this.entryTTL = entryTTL;
    this.expirationInterval = expirationInterval;
  }

  @Override
  public boolean contains(Serializable key) throws ObjectStoreException {
    return getStore().contains(key);
  }

  @Override
  public void store(Serializable key, T value) throws ObjectStoreException {
    Long time = Long.valueOf(System.currentTimeMillis());
    getStore().store(key, new StoredObject<T>(value, time, key));
  }

  @Override
  public T retrieve(Serializable key) throws ObjectStoreException {
    return getStore().retrieve(key).getItem();
  }

  @Override
  public void clear() throws ObjectStoreException {
    this.getStore().clear();
  }

  @Override
  public T remove(Serializable key) throws ObjectStoreException {
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
  public List<Serializable> allKeys() throws ObjectStoreException {
    return getStore().allKeys();
  }

  private ListableObjectStore<StoredObject<T>> getStore() {
    if (baseStore == null) {
      baseStore = context.getRegistry().lookupObject(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME);
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
      List<Serializable> keys = allKeys();
      int excess = (allKeys().size() - maxEntries);

      PriorityQueue<StoredObject<T>> sortedMaxEntries = null;

      if (excess > 0) {
        sortedMaxEntries =
            new PriorityQueue<StoredObject<T>>(excess, (paramT1, paramT2) -> paramT1.timestamp.compareTo(paramT2.timestamp));
      }

      ListableObjectStore<StoredObject<T>> store = getStore();
      for (Serializable key : keys) {
        StoredObject<T> obj = store.retrieve(key);

        if (entryTTL != UNBOUNDED && now - obj.getTimestamp() >= entryTTL) {
          remove(key);
          excess--;
        } else if (maxEntries != UNBOUNDED && excess > 0) {
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
      logger.warn("Running expirty on " + baseStore + " threw " + e + ":" + e.getMessage());
    }
  }

  @Override
  public void dispose() {
    if (scheduledTask != null) {
      scheduledTask.cancel(true);
      scheduler.shutdown();
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

  protected static class StoredObject<T> implements Serializable, DeserializationPostInitialisable {

    private static final long serialVersionUID = 8656763235928199259L;
    final private T item;
    final private Long timestamp;
    final private Serializable key;

    public StoredObject(T item, Long timestamp, Serializable key) {
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

    public Serializable getKey() {
      return key;
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.runtime.core.util.store.DeserializationPostInitialisable} is used. This will get invoked after the object
     * has been deserialized passing in the current MuleContext when using either
     * {@link org.mule.runtime.core.transformer.wire.SerializationWireFormat},
     * {@link org.mule.runtime.core.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.runtime.core.transformer.simple.ByteArrayToSerializable} transformer.
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
