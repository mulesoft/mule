/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.test.petstore.extension.PetstoreErrorTypeDefinition.PET_ERROR;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.api.streaming.exception.StreamingBufferSizeExceededException;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.security.AuthenticationHandler;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

public class PetStoreOperations {

  @Inject
  private ExtensionsClient client;

  public List<String> getPets(@Connection PetStoreClient client, @Config PetStoreConnector config, String ownerName) {
    return client.getPets(ownerName, config);
  }

  @Throws(PetStoreCustomErrorProvider.class)
  @MediaType(TEXT_PLAIN)
  public String failsToReadStream(@Connection PetStoreClient connection, @Optional String content) throws MuleException {
    try {
      if (content == null) {
        throw new Exception("Null content cannot be processed");
      }
      throw new StreamingBufferSizeExceededException(1);
    } catch (Exception e) {
      throw new ModuleException(PET_ERROR, e);
    }
  }

  public PetStoreClient getClient(@Connection PetStoreClient client) {
    return client;
  }

  @MediaType(TEXT_PLAIN)
  public String getFishFromRiverStream(@Content InputStream river, @Optional InputStream pollutedStream) {
    StringBuilder builder = new StringBuilder();
    builder.append(IOUtils.toString(river));
    if (pollutedStream != null) {
      builder.append(" ");
      builder.append(IOUtils.toString(pollutedStream));
    }
    return builder.toString();
  }

  @MediaType(TEXT_PLAIN)
  public String describeSerializedAnimal(@ParameterDsl(allowReferences = false) Serializable animal) {
    if (animal instanceof byte[]) {
      return new String((byte[]) animal);
    }
    return animal.toString();
  }

  public ExclusivePetBreeder getBreeder(@ParameterGroup(name = "Exclusive") ExclusivePetBreeder breeder) {
    return breeder;
  }

  public ExclusiveCashier getCashier(@ParameterGroup(name = "Exclusive") ExclusiveCashier cashier) {
    return cashier;
  }

  @MediaType(TEXT_PLAIN)
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

  public PetStoreConnector getConfig(@Config PetStoreConnector config) {
    return config;
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
