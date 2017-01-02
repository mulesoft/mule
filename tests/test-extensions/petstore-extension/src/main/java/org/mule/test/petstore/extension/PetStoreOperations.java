/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PetStoreOperations {

  public List<String> getPets(@Connection PetStoreClient client, @UseConfig PetStoreConnector config, String ownerName) {
    return client.getPets(ownerName, config);
  }

  public PetStoreClient getClient(@Connection PetStoreClient client) {
    return client;
  }

  public ExclusivePetBreeder getBreeder(@ParameterGroup(name = "Exclusive") ExclusivePetBreeder breeder) {
    return breeder;
  }

  public ExclusiveCashier getCashier(@ParameterGroup(name = "Exclusive") ExclusiveCashier cashier) {
    return cashier;
  }

  public String getDefaultEncoding(boolean usePhoneNumber, @Optional PhoneNumber phoneNumber,
                                   @DefaultEncoding String encoding) {
    return usePhoneNumber ? phoneNumber.getCountryEncoding() : encoding;
  }

  public List<Pet> getForbiddenPets(List<Pet> forbiddenPets) {
    return forbiddenPets;
  }

  public PetStoreClient getClientOnLatch(@Connection PetStoreClient client, Event event) throws Exception {
    CountDownLatch countDownLatch = (CountDownLatch) event.getVariable("testLatch").getValue();
    if (countDownLatch != null) {
      countDownLatch.countDown();
    }

    Latch latch = (Latch) event.getVariable("connectionLatch").getValue();
    latch.await();
    return client;
  }

  public PetCage getCage(@UseConfig PetStoreConnector config) {
    return config.getCage();
  }

  public void makePhoneCall(PhoneNumber phoneNumber) {}
}
