/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.store.ObjectStoreNotAvaliableException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

/**
 * <code>IdempotentMessageFilter</code> ensures that only unique messages are passed on. It does this by checking the unique ID of
 * the incoming message. Note that the underlying endpoint must support unique message IDs for this to work, otherwise a
 * <code>UniqueIdNotSupportedException</code> is thrown.<br>
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/IdempotentReceiver.html">
 * http://www.eaipatterns.com/IdempotentReceiver.html</a>
 */
public class IdempotentMessageFilter extends AbstractInterceptingMessageProcessor implements Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(IdempotentMessageFilter.class);

  protected volatile ObjectStore<String> store;
  protected String storePrefix;

  protected String idExpression = format("%sid%s", DEFAULT_EXPRESSION_PREFIX, DEFAULT_EXPRESSION_POSTFIX);
  protected String valueExpression = format("%sid%s", DEFAULT_EXPRESSION_PREFIX, DEFAULT_EXPRESSION_POSTFIX);

  public IdempotentMessageFilter() {
    super();
  }

  @Override
  public void initialise() throws InitialisationException {
    if (storePrefix == null) {
      storePrefix =
          format("%s.%s.%s", muleContext.getConfiguration().getId(), flowConstruct.getName(), this.getClass().getName());
    }
    if (store == null) {
      this.store = createMessageIdStore();
    }

    initialiseIfNeeded(store);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(store, LOGGER);
  }

  protected ObjectStore<String> createMessageIdStore() throws InitialisationException {
    ObjectStoreManager objectStoreManager = muleContext.getRegistry().get(OBJECT_STORE_MANAGER);
    return objectStoreManager.getObjectStore(storePrefix, false, -1, MINUTES.toMillis(5), SECONDS.toMillis(6));
  }

  protected String getValueForEvent(Event event) throws MessagingException {
    return muleContext.getExpressionManager().parse(valueExpression, event, flowConstruct);
  }

  protected String getIdForEvent(Event event) throws MuleException {
    return muleContext.getExpressionManager().parse(idExpression, event, flowConstruct);
  }

  public String getIdExpression() {
    return idExpression;
  }

  public void setIdExpression(String idExpression) {
    this.idExpression = idExpression;
  }

  public ObjectStore<String> getObjectStore() {
    return store;
  }

  public void setObjectStore(ObjectStore<String> store) {
    this.store = store;
  }

  private boolean accept(Event event) {
    if (event != null && isNewMessage(event)) {
      try {
        String id = getIdForEvent(event);
        String value = getValueForEvent(event);
        try {
          store.store(id, value);
          return true;
        } catch (ObjectAlreadyExistsException ex) {
          return false;
        } catch (ObjectStoreNotAvaliableException e) {
          logger.error("ObjectStore not available: " + e.getMessage());
          return false;
        } catch (ObjectStoreException e) {
          logger.warn("ObjectStore exception: " + e.getMessage());
          return false;
        }
      } catch (MuleException e) {
        logger.warn("Could not retrieve Id or Value for event: " + e.getMessage());
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public final Event process(Event event) throws MuleException {
    if (accept(event)) {
      return processNext(event);
    } else {
      event.getContext().success();
      return null;
    }
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).<Event>handle((event, sink) -> {
      if (accept(event)) {
        sink.next(event);
      } else {
        event.getContext().success();
      }
    }).transform(applyNext());
  }

  protected boolean isNewMessage(Event event) {
    try {
      String id = this.getIdForEvent(event);
      if (store == null) {
        synchronized (this) {
          initialise();
        }
      }
      return !store.contains(id);
    } catch (MuleException e) {
      logger.error("Exception attempting to determine idempotency of incoming message for " + flowConstruct.getName()
          + " from the connector " + event.getContext().getOriginatingConnectorName(), e);
      return false;
    }
  }

  public String getValueExpression() {
    return valueExpression;
  }

  public void setValueExpression(String valueExpression) {
    this.valueExpression = valueExpression;
  }

  public void setStorePrefix(String storePrefix) {
    this.storePrefix = storePrefix;
  }
}
