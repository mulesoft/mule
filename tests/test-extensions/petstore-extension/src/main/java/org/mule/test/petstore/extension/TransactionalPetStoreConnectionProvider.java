/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.Alias;

@Alias("transactional")
public class TransactionalPetStoreConnectionProvider extends PetStoreConnectionProvider<TransactionalPetStoreClient> {

  @Override
  public TransactionalPetStoreClient connect() {
    return new TransactionalPetStoreClient(username, password, tls, configName, openingDate, closedForHolidays, discountDates);
  }
}
