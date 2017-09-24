/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.lang.String.format;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.internal.util.rx.Operators.nullSafeMap;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.api.store.PartitionableObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.routing.correlation.EventCorrelator;
import org.mule.runtime.core.internal.routing.correlation.EventCorrelatorCallback;
import org.mule.runtime.core.internal.util.store.ProvidedObjectStoreWrapper;
import org.mule.runtime.core.internal.util.store.ProvidedPartitionableObjectStoreWrapper;
import org.mule.runtime.core.privileged.processor.AbstractInterceptingMessageProcessor;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * <code>AbstractEventAggregator</code> will aggregate a set of messages into a single message. <b>EIP Reference:</b>
 * <a href="http://www.eaipatterns.com/Aggregator.html" >http://www.eaipatterns.com/Aggregator.html</a>
 */

public abstract class AbstractAggregator extends AbstractInterceptingMessageProcessor
    implements Initialisable, MuleContextAware, Aggregator, Startable, Stoppable, Disposable {

  private static final Logger LOGGER = getLogger(AbstractAggregator.class);

  public static final int MAX_PROCESSED_GROUPS = 50000;

  protected EventCorrelator eventCorrelator;

  private long timeout = 0;
  private boolean failOnTimeout = true;

  private ObjectStore<Long> processedGroupsObjectStore;
  private PartitionableObjectStore eventGroupsObjectStore;

  protected boolean persistentStores;
  protected String storePrefix = null;

  protected String eventsObjectStoreKey;

  @Override
  public void initialise() throws InitialisationException {
    if (storePrefix == null) {
      storePrefix =
          format("%s%s.%s.", muleContext.getConfiguration().getId(), getLocation().getRootContainerName(),
                 this.getClass().getName());
    }

    initProcessedGroupsObjectStore();
    initEventGroupsObjectStore();

    eventCorrelator = new EventCorrelator(getCorrelatorCallback(muleContext), next, muleContext, getFlowConstruct(),
                                          eventGroupsObjectStore, storePrefix, processedGroupsObjectStore);

    eventCorrelator.setTimeout(timeout);
    eventCorrelator.setFailOnTimeout(isFailOnTimeout());
  }

  protected void initProcessedGroupsObjectStore() {
    if (processedGroupsObjectStore == null) {
      //TODO: Delete ProvidedObjectStoreWrapper if not needed when moving this to compatibility
      processedGroupsObjectStore = new ProvidedObjectStoreWrapper<>(null, internalProcessedGroupsObjectStoreFactory());
    }
  }

  private Supplier<ObjectStore> internalProcessedGroupsObjectStoreFactory() {
    return () -> {
      ObjectStoreManager objectStoreManager = ((MuleContextWithRegistries) muleContext).getRegistry().get(OBJECT_STORE_MANAGER);
      return objectStoreManager.createObjectStore(storePrefix + ".processedGroups", ObjectStoreSettings.builder()
          .persistent(persistentStores)
          .maxEntries(MAX_PROCESSED_GROUPS)
          .expirationInterval(1000L)
          .build());
    };
  }

  protected void initEventGroupsObjectStore() throws InitialisationException {
    try {
      if (eventGroupsObjectStore == null) {
        //TODO: Delete ProvidedObjectStoreWrapper if not needed when moving this to compatibility
        eventGroupsObjectStore = new ProvidedPartitionableObjectStoreWrapper(null, internalEventsGroupsObjectStoreSupplier());
      }
      eventGroupsObjectStore.open(storePrefix + ".expiredAndDispatchedGroups");
      eventGroupsObjectStore.open(storePrefix + ".eventGroups");
    } catch (MuleRuntimeException | ObjectStoreException e) {
      throw new InitialisationException(e, this);
    }
  }

  private Supplier<ObjectStore> internalEventsGroupsObjectStoreSupplier() {
    return () -> {
      ObjectStore objectStore;
      MuleRegistry registry = ((MuleContextWithRegistries) muleContext).getRegistry();
      if (persistentStores) {
        objectStore = registry.lookupObject(BASE_PERSISTENT_OBJECT_STORE_KEY);
      } else {
        objectStore = registry.lookupObject(BASE_IN_MEMORY_OBJECT_STORE_KEY);
      }
      if (objectStore instanceof MuleContextAware) {
        ((MuleContextAware) objectStore).setMuleContext(muleContext);
      }
      return objectStore;
    };
  }

  @Override
  public void start() throws MuleException {
    if (timeout != 0) {
      eventCorrelator.start();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (timeout != 0) {
      eventCorrelator.stop();
    }
  }

  protected abstract EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext);

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    CoreEvent result = eventCorrelator.process(event);
    if (result == null) {
      return null;
    }
    return processNext(result);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher).handle(nullSafeMap(checkedFunction(event -> process(event))));
  }

  @Override
  public void expireAggregation(String groupId) throws MuleException {
    eventCorrelator.forceGroupExpiry(groupId);
  }

  public long getTimeout() {
    return timeout;
  }

  @Override
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public boolean isFailOnTimeout() {
    return failOnTimeout;
  }

  @Override
  public void setFailOnTimeout(boolean failOnTimeout) {
    this.failOnTimeout = failOnTimeout;
  }

  public void setProcessedGroupsObjectStore(ObjectStore<Long> processedGroupsObjectStore) {
    this.processedGroupsObjectStore =
        //TODO: Delete ProvidedObjectStoreWrapper if not needed when moving this to compatibility
        new ProvidedObjectStoreWrapper<>(processedGroupsObjectStore, internalProcessedGroupsObjectStoreFactory());
  }

  public void setEventGroupsObjectStore(PartitionableObjectStore<CoreEvent> eventGroupsObjectStore) {
    this.eventGroupsObjectStore =
        //TODO: Delete ProvidedObjectStoreWrapper if not needed when moving this to compatibility
        new ProvidedPartitionableObjectStoreWrapper(eventGroupsObjectStore, internalEventsGroupsObjectStoreSupplier());
  }

  public boolean isPersistentStores() {
    return persistentStores;
  }

  public void setPersistentStores(boolean persistentStores) {
    this.persistentStores = persistentStores;
  }

  public String getStorePrefix() {
    return storePrefix;
  }

  public void setStorePrefix(String storePrefix) {
    this.storePrefix = storePrefix;
  }

  @Override
  public void dispose() {
    disposeIfNeeded(processedGroupsObjectStore, LOGGER);
    disposeIfNeeded(eventGroupsObjectStore, LOGGER);
  }
}
