/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.runtime.api.tx.TransactionException;

public interface Transaction {

  int STATUS_ACTIVE = 0;
  int STATUS_MARKED_ROLLBACK = 1;
  int STATUS_PREPARED = 2;
  int STATUS_COMMITTED = 3;
  int STATUS_ROLLEDBACK = 4;
  int STATUS_UNKNOWN = 5;
  int STATUS_NO_TRANSACTION = 6;
  int STATUS_PREPARING = 7;
  int STATUS_COMMITTING = 8;
  int STATUS_ROLLING_BACK = 9;

  /**
   * Begin the transaction.
   * 
   * @throws TransactionException
   */
  void begin() throws TransactionException;

  /**
   * Commit the transaction
   * 
   * @throws TransactionException
   */
  void commit() throws TransactionException;

  /**
   * Rollback the transaction
   * 
   * @throws TransactionException
   */
  void rollback() throws TransactionException;

  int getStatus() throws TransactionException;

  boolean isBegun() throws TransactionException;

  boolean isRolledBack() throws TransactionException;

  boolean isCommitted() throws TransactionException;

  /**
   * @return transaction timeout in milliseconds
   */
  int getTimeout();

  /**
   * @param timeout configures the transactions timeout in milliseconds
   */
  void setTimeout(int timeout);

  Object getResource(Object key);

  boolean hasResource(Object key);

  /**
   * @param key transactional resource key (i.e jdbc DataSource or jms Connection)
   * @param resource transactional resource (i.e. jdbc Connection or jms Session)
   * @return true if the current transaction supports to bind transactional resources key and resource
   */
  boolean supports(Object key, Object resource);

  void bindResource(Object key, Object resource) throws TransactionException;

  void setRollbackOnly() throws TransactionException;

  boolean isRollbackOnly() throws TransactionException;

  boolean isXA();

  /**
   * Resume the XA transaction
   *
   * @throws TransactionException if any error
   */
  void resume() throws TransactionException;

  /**
   * Suspend the XA transaction
   *
   * @throws TransactionException if any error
   */
  javax.transaction.Transaction suspend() throws TransactionException;

  /**
   * @return TX identification.
   */
  String getId();
}
