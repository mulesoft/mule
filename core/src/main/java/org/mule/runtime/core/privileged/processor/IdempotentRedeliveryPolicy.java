/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.initialisationFailure;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.MessageRedeliveredException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.event.DefaultEventContext;
import org.mule.runtime.core.internal.transformer.simple.ByteArrayToHexString;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.internal.util.store.ObjectStorePartition;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Implement a retry policy for Mule. This is similar to JMS retry policies that will redeliver a message a maximum number of
 * times. If this maximum is exceeded, fails with an exception.
 */
public class IdempotentRedeliveryPolicy extends AbstractRedeliveryPolicy {

  private final ObjectToByteArray objectToByteArray = new ObjectToByteArray();
  private final ByteArrayToHexString byteArrayToHexString = new ByteArrayToHexString();

  protected Logger logger = LoggerFactory.getLogger(this.getClass());

  private LockFactory lockFactory;
  private ObjectStoreManager objectStoreManager;
  private ExtendedExpressionManager expressionManager;

  private boolean useSecureHash;
  private String messageDigestAlgorithm;
  private String idExpression;
  private ObjectStore<AtomicInteger> store;
  private ObjectStore<AtomicInteger> privateStore;
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
      throw new InitialisationException(initialisationFailure(format("The message digest algorithm '%s' was specified when a secure hash will not be used",
                                                                     messageDigestAlgorithm)),
                                        this);
    }
    if (!useSecureHash && idExpression == null) {
      throw new InitialisationException(initialisationFailure("No method for identifying messages was specified"),
                                        this);
    }
    if (useSecureHash) {
      if (messageDigestAlgorithm == null) {
        messageDigestAlgorithm = "SHA-256";
      }
      try {
        MessageDigest.getInstance(messageDigestAlgorithm);
      } catch (NoSuchAlgorithmException e) {
        throw new InitialisationException(initialisationFailure(format("Exception '%s' initializing message digest algorithm %s",
                                                                       e.getMessage(), messageDigestAlgorithm)),
                                          this);

      }
    }

    idrId = format("%s-%s-%s", muleContext.getConfiguration().getId(), getLocation().getRootContainerName(), "idr");
    if (store != null && privateStore != null) {
      throw new InitialisationException(createStaticMessage("Ambiguous definition of object store, both reference and private were configured"),
                                        this);
    }
    if (store == null) {
      if (privateStore == null) //If no object store was defined, create one
      {
        this.store = internalObjectStoreSupplier().get();
      } else { //If object store was defined privately
        this.store = privateStore;
      }
    }
    initialiseIfNeeded(store, true, muleContext);
    initialiseIfNeeded(objectToByteArray, muleContext);
    initialiseIfNeeded(byteArrayToHexString, muleContext);
  }

  private Supplier<ObjectStore> internalObjectStoreSupplier() {
    return () -> {
      return objectStoreManager.createObjectStore(getLocation().getRootContainerName() + "." + getClass().getName(),
                                                  ObjectStoreSettings.builder()
                                                      .persistent(false)
                                                      .entryTtl((long) 60 * 5 * 1000)
                                                      .expirationInterval(6000L).build());
    };
  }

  @Override
  public void dispose() {
    super.dispose();
    if (store instanceof ObjectStorePartition) {
      try {
        ((ObjectStorePartition) store).close();
      } catch (ObjectStoreException e) {
        logger.warn("error closing object store: " + e.getMessage(), e);
      }
    }
    if (store != null) {
      disposeIfNeeded(store, logger);
      store = null;
    }
  }

  @Override
  public void start() throws MuleException {
    super.start();
    startIfNeeded(store);
  }

  @Override
  public void stop() throws MuleException {
    super.stop();
    stopIfNeeded(store);
  }


  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
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
        throw new MessageRedeliveredException(messageId, counter.get(), maxRedeliveryCount);
      }

      try {
        CoreEvent returnEvent =
            processNext(CoreEvent
                .builder(DefaultEventContext.child((BaseEventContext) event.getContext(), empty()), event).build());
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

  private String getIdForEvent(CoreEvent event) throws Exception {
    if (useSecureHash) {
      Object payload = event.getMessage().getPayload().getValue();
      byte[] bytes = (byte[]) objectToByteArray.transform(payload);
      if (payload instanceof InputStream) {
        // We've consumed the stream.
        event = CoreEvent.builder(event).message(Message.builder(event.getMessage()).value(bytes).build()).build();
      }
      MessageDigest md = MessageDigest.getInstance(messageDigestAlgorithm);
      byte[] digestedBytes = md.digest(bytes);
      return (String) byteArrayToHexString.transform(digestedBytes);
    } else {
      return expressionManager.parse(idExpression, event, getLocation());
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
    this.store = store;
  }

  public void setPrivateObjectStore(ObjectStore<AtomicInteger> store) {
    this.privateStore = store;
  }

  @Inject
  public void setLockFactory(LockFactory lockFactory) {
    this.lockFactory = lockFactory;
  }

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  public void setObjectStoreManager(ObjectStoreManager objectStoreManager) {
    this.objectStoreManager = objectStoreManager;
  }

  @Inject
  public void setExpressionManager(ExtendedExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

}

