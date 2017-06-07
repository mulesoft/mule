/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.security.AuthenticationHandler;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PetStoreOperations {

  public List<String> getPets(@Connection PetStoreClient client, @Config PetStoreConnector config, String ownerName) {
    return client.getPets(ownerName, config);
  }

  public PetStoreClient getClient(@Connection PetStoreClient client) {
    return client;
  }

  public String getFishFromRiverStream(@Content InputStream river, @Optional InputStream pollutedStream) {
    StringBuilder builder = new StringBuilder();
    builder.append(IOUtils.toString(river));
    if (pollutedStream != null) {
      builder.append(" ");
      builder.append(IOUtils.toString(pollutedStream));
    }
    return builder.toString();
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

  public PetStoreClient getClientOnLatch(@Connection PetStoreClient client, Object countDownLatch, Object latch)
      throws Exception {
    if (countDownLatch != null) {
      ((CountDownLatch) countDownLatch).countDown();
    }

    ((Latch) latch).await();
    return client;
  }

  public PetCage getCage(@Config PetStoreConnector config) {
    return config.getCage();
  }

  public void setSecureCage(@Optional @NullSafe List<String> providers, String user, String pass,
                            AuthenticationHandler authHandler)
      throws SecurityException, SecurityProviderNotFoundException, UnknownAuthenticationTypeException {

    authHandler.setAuthentication(providers,
                                  authHandler.createAuthentication(authHandler.createCredentialsBuilder()
                                      .withUsername(user)
                                      .withPassword(pass.toCharArray())
                                      .build()));
  }

  public void makePhoneCall(PhoneNumber phoneNumber) {}
}
