/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.initialisationFailure;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.internal.event.DefaultEventContext;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.internal.util.store.ObjectStorePartition;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.exception.MessageRedeliveredException;

import org.slf4j.Logger;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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

  public static final String SECURE_HASH_EXPR_FORMAT = "" +
      "%%dw 2.0" + lineSeparator() +
      "output text/plain" + lineSeparator() +
      "import dw::Crypto" + lineSeparator() +
      "---" + lineSeparator() +
      "Crypto::hashWith(payload, '%s')";

  private static final Logger logger = getLogger(IdempotentRedeliveryPolicy.class);

  private LockFactory lockFactory;
  private ObjectStoreManager objectStoreManager;
  private ExpressionManager expressionManager;

  private boolean useSecureHash;
  private String messageDigestAlgorithm;
  private String idExpression;
  private ObjectStore<RedeliveryCounter> store;
  private ObjectStore<RedeliveryCounter> privateStore;
  private String idrId;

  /**
   * Holds information about the redelivery failures.
   *
   * @since 4.0
   */
  public static class RedeliveryCounter implements Serializable {

    private static final long serialVersionUID = 5513487261745816555L;

    private AtomicInteger counter = new AtomicInteger();
    private List<Error> errors = new LinkedList<>();

  }

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

      idExpression = format(SECURE_HASH_EXPR_FORMAT, messageDigestAlgorithm);
    }

    idrId = format("%s-%s-%s", muleContext.getConfiguration().getId(), getLocation().getRootContainerName(), "idr");
    if (store != null && privateStore != null) {
      throw new InitialisationException(createStaticMessage("Ambiguous definition of object store, both reference and private were configured"),
                                        this);
    }
    if (store == null) {
      // If no object store was defined, create one
      if (privateStore == null) {
        this.store = internalObjectStoreSupplier().get();
      } else {
        // If object store was defined privately
        this.store = privateStore;
      }
    }
    initialiseIfNeeded(store, true, muleContext);
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
    Optional<Exception> exceptionSeen = empty();

    String messageId = null;
    try {
      messageId = getIdForEvent(event);
    } catch (ExpressionRuntimeException e) {
      logger
          .warn("The message cannot be processed because the digest could not be generated. Either make the payload serializable or use an expression.");
      return null;
    } catch (Exception ex) {
      exceptionSeen = of(ex);
    }

    Lock lock = lockFactory.createLock(idrId + "-" + messageId);
    lock.lock();
    try {

      RedeliveryCounter counter = findCounter(messageId);
      if (exceptionSeen.isPresent()) {
        throw new MessageRedeliveredException(messageId, counter.counter.get(), maxRedeliveryCount, exceptionSeen.get());
      } else if (counter != null && counter.counter.get() > maxRedeliveryCount) {
        throw new MessageRedeliveredException(messageId, counter.errors, counter.counter.get(), maxRedeliveryCount);
      }

      try {
        CoreEvent returnEvent = processNext(CoreEvent
            .builder(DefaultEventContext.child((BaseEventContext) event.getContext(), empty()), event).build());
        counter = findCounter(messageId);
        if (counter != null) {
          resetCounter(messageId);
        }
        return returnEvent;
      } catch (Exception ex) {
        if (ex instanceof MessagingException) {
          incrementCounter(messageId, (MessagingException) ex);
          throw ex;
        } else {
          MessagingException me = createMessagingException(event, ex, this);
          incrementCounter(messageId, me);
          throw ex;
        }
      }
    } finally {
      lock.unlock();
    }
  }

  private MessagingException createMessagingException(CoreEvent event, Throwable cause, Component processor) {
    MessagingExceptionResolver exceptionResolver = new MessagingExceptionResolver(processor);
    MessagingException me = new MessagingException(event, cause, processor);

    return exceptionResolver.resolve(me, muleContext);
  }

  private void resetCounter(String messageId) throws ObjectStoreException {
    store.remove(messageId);
    store.store(messageId, new RedeliveryCounter());
  }

  public RedeliveryCounter findCounter(String messageId) throws ObjectStoreException {
    boolean counterExists = store.contains(messageId);
    if (counterExists) {
      return store.retrieve(messageId);
    }
    return null;
  }

  private RedeliveryCounter incrementCounter(String messageId, MessagingException ex) throws ObjectStoreException {
    RedeliveryCounter counter = findCounter(messageId);
    if (counter == null) {
      counter = new RedeliveryCounter();
    } else {
      store.remove(messageId);
    }
    counter.counter.incrementAndGet();
    counter.errors.add(ex.getEvent().getError().get());
    store.store(messageId, counter);
    return counter;
  }

  private String getIdForEvent(CoreEvent event) {
    return (String) expressionManager.evaluate(idExpression, STRING, NULL_BINDING_CONTEXT, event).getValue();
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

  public void setObjectStore(ObjectStore<RedeliveryCounter> store) {
    this.store = store;
  }

  public void setPrivateObjectStore(ObjectStore<RedeliveryCounter> store) {
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
  public void setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

}

