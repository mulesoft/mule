/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.Alias;

@Alias("transactional")
public class TransactionalPetStoreConnectionProvider extends PetStoreConnectionProvider<TransactionalPetStoreClient> {

  @Override
  public TransactionalPetStoreClient connect() {
    return new TransactionalPetStoreClient(username, password, tls, configName, openingDate, closedForHolidays, discountDates,
                                           muleVersion);
  }
}
