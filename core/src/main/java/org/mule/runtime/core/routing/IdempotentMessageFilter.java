/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.lang.String.format;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.store.ObjectStoreNotAvaliableException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.AbstractFilteringMessageProcessor;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>IdempotentMessageFilter</code> ensures that only unique messages are passed on. It does this by checking the unique ID of
 * the incoming message. Note that the underlying endpoint must support unique message IDs for this to work, otherwise a
 * <code>UniqueIdNotSupportedException</code> is thrown.<br>
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/IdempotentReceiver.html">
 * http://www.eaipatterns.com/IdempotentReceiver.html</a>
 */
public class IdempotentMessageFilter extends AbstractFilteringMessageProcessor implements Initialisable, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdempotentMessageFilter.class);

  protected volatile ObjectStore<String> store;
  protected String storePrefix;

  protected String idExpression = MessageFormat.format("{0}message:id{1}", ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                                                       ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

  protected String valueExpression = MessageFormat.format("{0}message:id{1}", ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                                                          ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

  public IdempotentMessageFilter() {
    super();
  }

  @Override
  public void initialise() throws InitialisationException {
    if (storePrefix == null) {
      storePrefix = format("%s.%s.%s", getPrefix(muleContext), flowConstruct.getName(), this.getClass().getName());
    }
    if (store == null) {
      this.store = createMessageIdStore();
    }

    LifecycleUtils.initialiseIfNeeded(store);
  }

  @Override
  public void dispose() {
    LifecycleUtils.disposeIfNeeded(store, LOGGER);
  }

  protected ObjectStore<String> createMessageIdStore() throws InitialisationException {
    ObjectStoreManager objectStoreManager = muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER);
    return objectStoreManager.getObjectStore(storePrefix, false, -1, 60 * 5 * 1000, 6000);
  }

  @Override
  protected MuleEvent processNext(MuleEvent event) throws MuleException {
    return super.processNext(event);
  }

  protected String getValueForEvent(MuleEvent event) throws MessagingException {
    return flowConstruct.getMuleContext().getExpressionManager().parse(valueExpression, event, flowConstruct, true);
  }

  protected String getIdForEvent(MuleEvent event) throws MessagingException {
    return flowConstruct.getMuleContext().getExpressionManager().parse(idExpression, event, flowConstruct, true);
  }

  public String getIdExpression() {
    return idExpression;
  }

  public void setIdExpression(String idExpression) {
    this.idExpression = idExpression;
  }

  public ObjectStore<String> getStore() {
    return store;
  }

  public void setStore(ObjectStore<String> store) {
    this.store = store;
  }

  @Override
  protected boolean accept(MuleEvent event, MuleEvent.Builder builder) {
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
      } catch (MessagingException e) {
        logger.warn("Could not retrieve Id or Value for event: " + e.getMessage());
        return false;
      }
    } else {
      return false;
    }
  }

  protected boolean isNewMessage(MuleEvent event) {
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
