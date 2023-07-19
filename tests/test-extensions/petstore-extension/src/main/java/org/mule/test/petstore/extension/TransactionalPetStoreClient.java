/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class TransactionalPetStoreClient extends PetStoreClient implements TransactionalConnection {

  private boolean begun, commited, rolledback = false;

  public TransactionalPetStoreClient(String username, String password, TlsContextFactory tlsContextFactory,
                                     String configName, Date openingDate, List<Date> closedForHolidays,
                                     List<LocalDateTime> discountDates, MuleVersion muleVersion) {
    super(username, password, tlsContextFactory, configName, openingDate, closedForHolidays, discountDates, muleVersion);
  }

  @Override
  public void begin() throws TransactionException {
    begun = true;
  }

  @Override
  public void commit() throws TransactionException {
    commited = true;
  }

  @Override
  public void rollback() throws TransactionException {
    rolledback = true;
  }

  public boolean isBegun() {
    return begun;
  }

  public boolean isCommited() {
    return commited;
  }

  public boolean isRolledback() {
    return rolledback;
  }
}
