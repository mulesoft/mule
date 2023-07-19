/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transactional.connection;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.sdk.api.connectivity.TransactionalConnection;

public class SdkTestLocalTransactionalConnection implements SdkTestTransactionalConnection, TransactionalConnection {

  private boolean transactionBegun, transactionCommited, transactionRolledback = false;
  private boolean connected = true;
  private double connectionId;

  public SdkTestLocalTransactionalConnection() {
    connectionId = Math.random();
  }

  @Override
  public void begin() throws TransactionException {
    transactionBegun = true;
  }

  @Override
  public void commit() throws TransactionException {
    transactionCommited = true;
  }

  @Override
  public void rollback() throws TransactionException {
    transactionRolledback = true;
  }

  @Override
  public double getConnectionId() {
    return connectionId;
  }

  @Override
  public boolean isTransactionBegun() {
    return transactionBegun;
  }

  @Override
  public boolean isTransactionCommited() {
    return transactionCommited;
  }

  @Override
  public boolean isTransactionRolledback() {
    return transactionRolledback;
  }

  @Override
  public void disconnect() {
    connected = false;
  }

  @Override
  public boolean isConnected() {
    return connected;
  }
}
