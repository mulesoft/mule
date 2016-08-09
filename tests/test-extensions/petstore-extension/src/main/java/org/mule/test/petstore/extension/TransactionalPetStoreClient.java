/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;

import java.util.Date;

public class TransactionalPetStoreClient extends PetStoreClient implements TransactionalConnection {

  private boolean begun, commited, rolledback = false;

  public TransactionalPetStoreClient(String username, String password, TlsContextFactory tlsContextFactory,
                                     ThreadingProfile threadingProfile, String configName, Date openingDate) {
    super(username, password, tlsContextFactory, threadingProfile, configName, openingDate);
  }

  @Override
  public void begin() throws Exception {
    begun = true;
  }

  @Override
  public void commit() throws Exception {
    commited = true;
  }

  @Override
  public void rollback() throws Exception {
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
