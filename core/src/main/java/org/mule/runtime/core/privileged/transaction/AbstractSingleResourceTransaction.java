/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transaction;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.TransactionStatusException;
import org.mule.runtime.core.privileged.transaction.xa.IllegalTransactionStateException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.transaction.Status;

/**
 * This abstract class can be used as a base class for transactions that can enlist only one resource (such as a JMS session or
 * JDBC connection).
 */
public abstract class AbstractSingleResourceTransaction extends AbstractTransaction {

  /**
   * TX status code to human-readable string mappings.
   *
   * @see javax.transaction.Status
   */
  protected static Map<Integer, String> txStatusMappings = new HashMap<Integer, String>(10); // populated later

  protected volatile Object key;
  protected volatile Object resource;

  protected final AtomicBoolean started = new AtomicBoolean(false);
  protected final AtomicBoolean committed = new AtomicBoolean(false);
  protected final AtomicBoolean rolledBack = new AtomicBoolean(false);
  protected final AtomicBoolean rollbackOnly = new AtomicBoolean(false);

  static {
    Field[] fields = Status.class.getFields();
    for (Field field : fields) {
      try {
        txStatusMappings.put(field.getInt(Status.class), field.getName());
      } catch (IllegalAccessException e) {
        // ignore
      }
    }

    txStatusMappings = Collections.unmodifiableMap(txStatusMappings);
  }

  protected AbstractSingleResourceTransaction(MuleContext muleContext) {
    super(muleContext);
  }

  public void begin() throws TransactionException {
    super.begin();
    started.compareAndSet(false, true);
  }

  public void commit() throws TransactionException {
    super.commit();
    committed.compareAndSet(false, true);
  }

  public void rollback() throws TransactionException {
    super.rollback();
    rolledBack.compareAndSet(false, true);
  }

  public int getStatus() throws TransactionStatusException {
    if (rolledBack.get()) {
      return STATUS_ROLLEDBACK;
    }
    if (committed.get()) {
      return STATUS_COMMITTED;
    }
    if (rollbackOnly.get()) {
      return STATUS_MARKED_ROLLBACK;
    }
    if (started.get()) {
      return STATUS_ACTIVE;
    }
    return STATUS_NO_TRANSACTION;
  }

  public Object getResource(Object key) {
    return key != null && this.key == key ? this.resource : null;
  }

  public boolean hasResource(Object key) {
    return key != null && this.key == key;
  }

  public void bindResource(Object key, Object resource) throws TransactionException {
    if (key == null) {
      throw new IllegalTransactionStateException(CoreMessages.transactionCannotBindToNullKey());
    }
    if (resource == null) {
      throw new IllegalTransactionStateException(CoreMessages.transactionCannotBindNullResource());
    }
    if (this.key != null) {
      throw new IllegalTransactionStateException(CoreMessages.transactionSingleResourceOnly());
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Binding " + resource + " to " + key);
    }

    this.key = key;
    this.resource = resource;
  }

  public void setRollbackOnly() {
    rollbackOnly.set(true);
  }

  @Override
  public String toString() {
    int status;
    try {
      status = getStatus();
    } catch (TransactionException e) {
      status = -1;
    }

    // map status to a human-readable string

    String statusName = txStatusMappings.get(status);
    if (statusName == null) {
      statusName = "*undefined*";
    }

    return new StringBuilder().append(getClass().getName())
        .append('@').append(id)
        .append("[status=").append(statusName)
        .append(", key=").append(key)
        .append("]").toString();
  }

  @Override
  public boolean supports(Object key, Object resource) {
    return (this.key == null
        && (getKeyType().isAssignableFrom(key.getClass()) && getResourceType().isAssignableFrom(resource.getClass())))
        || (this.key != null && (this.key == key && this.resource == resource));
  }

  protected Class getResourceType() {
    throw new MuleRuntimeException(CoreMessages
        .createStaticMessage("Transaction type: " + this.getClass().getName() + " doesn't support supports(..) method"));
  }

  protected Class getKeyType() {
    throw new MuleRuntimeException(CoreMessages
        .createStaticMessage("Transaction type: " + this.getClass().getName() + " doesn't support supports(..) method"));
  }
}
