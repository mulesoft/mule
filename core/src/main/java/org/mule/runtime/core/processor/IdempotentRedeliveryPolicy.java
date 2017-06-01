/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessageRedeliveredException;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.transformer.simple.ByteArrayToHexString;
import org.mule.runtime.core.util.store.ObjectStorePartition;
import org.mule.runtime.core.util.store.ProvidedObjectStoreWrapper;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import org.apache.commons.collections.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement a retry policy for Mule. This is similar to JMS retry policies that will redeliver a message a maximum number of
 * times. If this maximum is exceeded, fails with an exception.
 */
public class IdempotentRedeliveryPolicy extends AbstractRedeliveryPolicy {

  private final ObjectToByteArray objectToByteArray = new ObjectToByteArray();
  private final ByteArrayToHexString byteArrayToHexString = new ByteArrayToHexString();

  protected Logger logger = LoggerFactory.getLogger(this.getClass());

  private boolean useSecureHash;
  private String messageDigestAlgorithm;
  private String idExpression;
  private ObjectStore<AtomicInteger> store;
  private LockFactory lockFactory;
  private String idrId;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    if (useSecureHash && idExpression != null) {
      useSecureHash = false;
      if (logger.isWarnEnabled()) {
        logger.warn("Disabling useSecureHash in idempotent-redelivery-policy since an idExpression has been configured");
      }
    }
    if (!useSecureHash && messageDigestAlgorithm != null) {
      throw new InitialisationException(CoreMessages.initialisationFailure(String
          .format("The message digest algorithm '%s' was specified when a secure hash will not be used", messageDigestAlgorithm)),
                                        this);
    }
    if (!useSecureHash && idExpression == null) {
      throw new InitialisationException(CoreMessages.initialisationFailure("No method for identifying messages was specified"),
                                        this);
    }
    if (useSecureHash) {
      if (messageDigestAlgorithm == null) {
        messageDigestAlgorithm = "SHA-256";
      }
      try {
        MessageDigest.getInstance(messageDigestAlgorithm);
      } catch (NoSuchAlgorithmException e) {
        throw new InitialisationException(CoreMessages.initialisationFailure(String
            .format("Exception '%s' initializing message digest algorithm %s", e.getMessage(), messageDigestAlgorithm)), this);

      }
    }

    String appName = muleContext.getConfiguration().getId();
    String flowName = flowConstruct.getName();
    idrId = String.format("%s-%s-%s", appName, flowName, "idr");
    lockFactory = muleContext.getLockFactory();
    if (store == null) {
      store = new ProvidedObjectStoreWrapper<>(null, internalObjectStoreFactory());
    }
    initialiseIfNeeded(objectToByteArray, muleContext);
    initialiseIfNeeded(byteArrayToHexString, muleContext);
  }

  private Factory internalObjectStoreFactory() {
    return () -> {
      ObjectStoreManager objectStoreManager = muleContext.getObjectStoreManager();
      return objectStoreManager.getObjectStore(flowConstruct.getName() + "." + getClass().getName(), false, -1, 60 * 5 * 1000,
                                               6000);
    };
  }

  @Override
  public void dispose() {
    super.dispose();

    if (store != null) {
      if (store instanceof ObjectStorePartition) {
        try {
          ((ObjectStorePartition) store).close();
        } catch (ObjectStoreException e) {
          logger.warn("error closing object store: " + e.getMessage(), e);
        }
      }
      store = null;
    }
  }

  @Override
  public void start() throws MuleException {}


  @Override
  public Event process(Event event) throws MuleException {
    boolean exceptionSeen = false;
    boolean tooMany = false;
    AtomicInteger counter = null;

    String messageId = null;
    try {
      messageId = getIdForEvent(event);
    } catch (TransformerException e) {
      logger
          .warn("The message cannot be processed because the digest could not be generated. Either make the payload serializable or use an expression.");
      return null;
    } catch (Exception ex) {
      exceptionSeen = true;
    }

    Lock lock = lockFactory.createLock(idrId + "-" + messageId);
    lock.lock();
    try {

      if (!exceptionSeen) {
        counter = findCounter(messageId);
        tooMany = counter != null && counter.get() > maxRedeliveryCount;
      }

      if (tooMany || exceptionSeen) {
        throw new MessageRedeliveredException(messageId, counter.get(), maxRedeliveryCount, event,
                                              CoreMessages.createStaticMessage("Redelivery exhausted"), this);
      }

      try {
        Event returnEvent = processNext(event);
        counter = findCounter(messageId);
        if (counter != null) {
          resetCounter(messageId);
        }
        return returnEvent;
      } catch (MuleException ex) {
        incrementCounter(messageId);
        throw ex;
      } catch (RuntimeException ex) {
        incrementCounter(messageId);
        throw ex;
      }
    } finally {
      lock.unlock();
    }

  }

  private void resetCounter(String messageId) throws ObjectStoreException {
    store.remove(messageId);
    store.store(messageId, new AtomicInteger());
  }

  public AtomicInteger findCounter(String messageId) throws ObjectStoreException {
    boolean counterExists = store.contains(messageId);
    if (counterExists) {
      return store.retrieve(messageId);
    }
    return null;
  }

  private AtomicInteger incrementCounter(String messageId) throws ObjectStoreException {
    AtomicInteger counter = findCounter(messageId);
    if (counter == null) {
      counter = new AtomicInteger();
    } else {
      store.remove(messageId);
    }
    counter.incrementAndGet();
    store.store(messageId, counter);
    return counter;
  }

  private String getIdForEvent(Event event) throws Exception {
    if (useSecureHash) {
      Object payload = event.getMessage().getPayload().getValue();
      byte[] bytes = (byte[]) objectToByteArray.transform(payload);
      if (payload instanceof InputStream) {
        // We've consumed the stream.
        event = Event.builder(event).message(Message.builder(event.getMessage()).payload(bytes).build()).build();
      }
      MessageDigest md = MessageDigest.getInstance(messageDigestAlgorithm);
      byte[] digestedBytes = md.digest(bytes);
      return (String) byteArrayToHexString.transform(digestedBytes);
    } else {
      return muleContext.getExpressionManager().parse(idExpression, event, flowConstruct);
    }
  }

  public boolean isUseSecureHash() {
    return useSecureHash;
  }

  public void setUseSecureHash(boolean useSecureHash) {
    this.useSecureHash = useSecureHash;
  }

  public String getMessageDigestAlgorithm() {
    return messageDigestAlgorithm;
  }

  public void setMessageDigestAlgorithm(String messageDigestAlgorithm) {
    this.messageDigestAlgorithm = messageDigestAlgorithm;
  }

  public String getIdExpression() {
    return idExpression;
  }

  public void setIdExpression(String idExpression) {
    this.idExpression = idExpression;
  }

  public void setObjectStore(ObjectStore<AtomicInteger> store) {
    this.store = new ProvidedObjectStoreWrapper<>(store, internalObjectStoreFactory());
  }
}

