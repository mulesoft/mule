/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.lang.String.format;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME;
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
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.routing.Aggregator;
import org.mule.runtime.core.api.store.PartitionableObjectStore;
import org.mule.runtime.core.internal.util.store.ProvidedObjectStoreWrapper;
import org.mule.runtime.core.internal.util.store.ProvidedPartitionableObjectStoreWrapper;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.routing.correlation.EventCorrelator;
import org.mule.runtime.core.routing.correlation.EventCorrelatorCallback;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

/**
 * <code>AbstractEventAggregator</code> will aggregate a set of messages into a single message. <b>EIP Reference:</b>
 * <a href="http://www.eaipatterns.com/Aggregator.html" >http://www.eaipatterns.com/Aggregator.html</a>
 */

public abstract class AbstractAggregator extends AbstractInterceptingMessageProcessor
    implements Initialisable, MuleContextAware, FlowConstructAware, Aggregator, Startable, Stoppable, Disposable {

  private static final Logger LOGGER = getLogger(AbstractAggregator.class);

  public static final int MAX_PROCESSED_GROUPS = 50000;
  public static final String EVENTS_STORE_REGISTRY_KEY_PREFIX = "aggregator.eventsObjectStore.";

  protected EventCorrelator eventCorrelator;
  protected MuleContext muleContext;

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
      storePrefix = format("%s%s.%s.", muleContext.getConfiguration().getId(), getLocation().getParts().get(0).getPartPath(),
                           this.getClass().getName());
    }

    initProcessedGroupsObjectStore();
    initEventGroupsObjectStore();

    eventCorrelator = new EventCorrelator(getCorrelatorCallback(muleContext), next, muleContext, flowConstruct,
                                          eventGroupsObjectStore, storePrefix, processedGroupsObjectStore);

    eventCorrelator.setTimeout(timeout);
    eventCorrelator.setFailOnTimeout(isFailOnTimeout());
  }

  protected void initProcessedGroupsObjectStore() {
    if (processedGroupsObjectStore == null) {
      processedGroupsObjectStore = new ProvidedObjectStoreWrapper<>(null, internalProcessedGroupsObjectStoreFactory());
    }
  }

  private Supplier<ObjectStore> internalProcessedGroupsObjectStoreFactory() {
    return () -> {
      ObjectStoreManager objectStoreManager = muleContext.getRegistry().get(OBJECT_STORE_MANAGER);
      return objectStoreManager.getObjectStore(storePrefix + ".processedGroups", persistentStores, MAX_PROCESSED_GROUPS, -1,
                                               1000);
    };
  }

  protected void initEventGroupsObjectStore() throws InitialisationException {
    try {
      if (eventGroupsObjectStore == null) {
        eventGroupsObjectStore = new ProvidedPartitionableObjectStoreWrapper<>(null, internalEventsGroupsObjectStoreSupplier());
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
      if (persistentStores) {
        objectStore = muleContext.getRegistry().lookupObject(OBJECT_STORE_DEFAULT_PERSISTENT_NAME);
      } else {
        objectStore = muleContext.getRegistry().lookupObject(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME);
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

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  protected abstract EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext);

  @Override
  public Event process(Event event) throws MuleException {
    Event result = eventCorrelator.process(event);
    if (result == null) {
      return null;
    }
    return processNext(result);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
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
        new ProvidedObjectStoreWrapper<>(processedGroupsObjectStore, internalProcessedGroupsObjectStoreFactory());
  }

  public void setEventGroupsObjectStore(PartitionableObjectStore<Event> eventGroupsObjectStore) {
    this.eventGroupsObjectStore =
        new ProvidedPartitionableObjectStoreWrapper<>(eventGroupsObjectStore, internalEventsGroupsObjectStoreSupplier());
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
