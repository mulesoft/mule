/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.internal.el.BindingContextUtils.CORRELATION_ID;
import static org.mule.runtime.internal.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreNotAvailableException;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.DuplicateMessageException;

import org.slf4j.Logger;

/**
 * <code>IdempotentMessageValidator</code> ensures that only unique messages are passed on. It does this by checking the unique ID
 * of the incoming message. To compute the unique ID an expression or DW script can be used, even Crypto functions from DW capable
 * of computing hashes(SHA,MD5) from the data. Note that the underlying endpoint must support unique message IDs for this to work,
 * otherwise a <code>UniqueIdNotSupportedException</code> is thrown.<br>
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/IdempotentReceiver.html">
 * http://www.eaipatterns.com/IdempotentReceiver.html</a>
 */
public class IdempotentMessageValidator extends AbstractAnnotatedObject
    implements Processor, MuleContextAware, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(IdempotentMessageValidator.class);

  protected MuleContext muleContext;

  protected volatile ObjectStore<String> store;
  protected String storePrefix;

  protected String idExpression = format("%s%s%s", DEFAULT_EXPRESSION_PREFIX, CORRELATION_ID, DEFAULT_EXPRESSION_POSTFIX);
  protected String valueExpression = format("%s%s%s", DEFAULT_EXPRESSION_PREFIX, CORRELATION_ID, DEFAULT_EXPRESSION_POSTFIX);

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (storePrefix == null) {
      storePrefix =
          format("%s.%s.%s", muleContext.getConfiguration().getId(), getLocation().getRootContainerName(),
                 this.getClass().getName());
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
    return objectStoreManager.createObjectStore(storePrefix, ObjectStoreSettings.builder()
        .persistent(false)
        .entryTtl(MINUTES.toMillis(5))
        .expirationInterval(SECONDS.toMillis(6))
        .build());
  }

  protected String getValueForEvent(Event event) throws MessagingException {
    return (String) muleContext.getExpressionManager().evaluate(valueExpression, STRING, NULL_BINDING_CONTEXT, event).getValue();
  }

  protected String getIdForEvent(Event event) throws MuleException {
    return (String) muleContext.getExpressionManager().evaluate(idExpression, STRING, NULL_BINDING_CONTEXT, event).getValue();
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
        } catch (ObjectStoreNotAvailableException e) {
          LOGGER.error("ObjectStore not available: " + e.getMessage());
          return false;
        } catch (ObjectStoreException e) {
          LOGGER.warn("ObjectStore exception: " + e.getMessage());
          return false;
        }
      } catch (MuleException e) {
        LOGGER.warn("Could not retrieve Id or Value for event: " + e.getMessage());
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public final Event process(Event event) throws MuleException {
    if (accept(event)) {
      return event;
    } else {
      throw new DuplicateMessageException();
    }
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
      LOGGER.error("Exception attempting to determine idempotency of incoming message for " + getLocation().getRootContainerName()
          + " from the connector "
          + event.getContext().getOriginatingLocation().getComponentIdentifier().getIdentifier().getNamespace(), e);
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
