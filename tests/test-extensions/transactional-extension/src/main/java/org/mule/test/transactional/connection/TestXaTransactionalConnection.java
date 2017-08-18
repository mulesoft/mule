/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional.connection;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;

import javax.transaction.xa.XAResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestXaTransactionalConnection implements TestTransactionalConnection, XATransactionalConnection {

  private static final Logger logger = LoggerFactory.getLogger(TestXaTransactionalConnection.class);
  private DummyXaResource xaResource;
  private double connectionId;

  TestXaTransactionalConnection(DummyXaResource xaResource) {
    this.xaResource = xaResource;
    this.connectionId = Math.random();
  }

  @Override
  public void begin() throws TransactionException {
    logger.debug("Begin Conn Transaction");
  }

  @Override
  public void commit() throws TransactionException {
    logger.debug("Commit Conn Transaction");
  }

  @Override
  public void rollback() throws TransactionException {
    logger.debug("Rollback Conn Transaction");
  }

  @Override
  public XAResource getXAResource() {
    logger.debug("Giving XA Resource");
    return xaResource;
  }

  @Override
  public void close() {
    logger.debug("Closing XA Transaction");
  }

  @Override
  public double getConnectionId() {
    return connectionId;
  }

  @Override
  public boolean isTransactionBegun() {
    return xaResource.isTxStarted();
  }

  @Override
  public boolean isTransactionCommited() {
    return xaResource.isCommitStarted();
  }

  @Override
  public boolean isTransactionRolledback() {
    return xaResource.isRollbackExecuted();
  }

  @Override
  public void disconnect() {}

  @Override
  public boolean isConnected() {
    return false;
  }
}
