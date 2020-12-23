/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static java.lang.String.format;
import static org.mule.runtime.core.api.config.FeatureFlaggingRegistry.getInstance;
import static org.mule.runtime.core.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.test.petstore.extension.PetStoreFeatures.LEGACY_FEATURE;
import static org.mule.test.petstore.extension.PetstoreErrorTypeDefinition.PET_ERROR;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.api.streaming.exception.StreamingBufferSizeExceededException;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.security.AuthenticationHandler;
import org.mule.runtime.extension.api.stereotype.ValidatorStereotype;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PetStoreOperations {

  static {
    // Register a feature that behaves differently with runtime versions older than 4.2.2
    // noinspection deprecation
    getInstance()
        .registerFeature(LEGACY_FEATURE,
                         c -> c.getConfiguration().getMinMuleVersion().isPresent()
                             && !c.getConfiguration().getMinMuleVersion().get().newerThan("4.2.2"));
  }

  @Inject
  @Named(FEATURE_FLAGGING_SERVICE_KEY)
  private FeatureFlaggingService ffService;

  public static boolean shouldFailWithConnectionException;
  public static AtomicInteger operationExecutionCounter = new AtomicInteger(0);

  public Long getConnectionAge(@Connection PetStoreClient client,
                               @Config PetStoreConnector config) {
    return System.currentTimeMillis() - client.getTimeOfCreation();
  }

  @MediaType(ANY)
  public void scopeWithMuleStereotype(@AllowedStereotypes(ValidatorStereotype.class) Chain validators,
                                      CompletionCallback<String, String> completionCallback) {
    completionCallback.success((Result.<String, String>builder().output("Ok").attributes("Attributes").build()));
  }

  @MediaType(TEXT_PLAIN)
  public String echoWithSignature(String message) {
    return message + " echoed by Petstore";
  }

  @MediaType(TEXT_PLAIN)
  public String featureFlaggedEcho(String message) {
    // noinspection deprecation
    if (ffService.isEnabled(LEGACY_FEATURE)) {
      return format("%s [old way]", message);
    }
    return message;
  }

  public List<String> getPets(@Connection PetStoreClient client,
                              @Config PetStoreConnector config,
                              String ownerName,
                              @Optional InputStream ownerSignature) {

    if (ownerSignature != null) {
      ownerName += IOUtils.toString(ownerSignature);
    }

    return client.getPets(ownerName, config);
  }

  @MediaType(TEXT_PLAIN)
  public InputStream getStreamedSignature(String signature) {
    return new ByteArrayInputStream(signature.getBytes());
  }

  @MediaType(TEXT_PLAIN)
  public InputStream getStreamedSignatureWithError(@Connection PetStoreClient petStoreClient, String signature) {
    throw new IllegalStateException("The operation failed!");
  }

  public List<String> getPetsWithIntermitentConnectionProblemAndClosingStream(@Connection PetStoreClient client,
                                                                              @Config PetStoreConnector config,
                                                                              String ownerName,
                                                                              @Optional InputStream ownerSignature)
      throws IOException {
    operationExecutionCounter.incrementAndGet();
    if (ownerSignature != null) {
      ownerName += IOUtils.toString(ownerSignature);
    }
    shouldFailWithConnectionException = !shouldFailWithConnectionException;
    if (!shouldFailWithConnectionException) {
      ownerSignature.close();
      throw new RuntimeException(new ConnectionException("kaboom"));
    }
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

  @OutputResolver(output = CorrelationInfoOutputResolver.class)
  public CorrelationInfo getPetCorrelation(CorrelationInfo correlationInfo) {
    return correlationInfo;
  }

  public ExclusivePetBreeder getBreeder(@ParameterGroup(name = "Exclusive") ExclusivePetBreeder breeder) {
    return breeder;
  }

  public ExclusiveCashier getCashier(@ParameterGroup(name = "Exclusive") ExclusiveCashier cashier) {
    return cashier;
  }

  public Aquarium getAquarium(Aquarium aquarium) {
    return aquarium;
  }

  public PetStoreDeal getPetStoreDeal(PetStoreDeal petStoreDeal) {
    return petStoreDeal;
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

  @MediaType(ANY)
  public Character spellObject(Character character) {
    return character;
  }

  @MediaType(ANY)
  public char spellBuiltIn(char character) {
    return character;
  }

  @MediaType(ANY)
  public Class spellClass(Class clazz) {
    return clazz;
  }

  public static class CorrelationInfoOutputResolver implements OutputTypeResolver<CorrelationInfo> {

    @Override
    public MetadataType getOutputType(MetadataContext context, CorrelationInfo key) {
      return context.getTypeLoader().load(CorrelationInfo.class);
    }

    @Override
    public String getCategoryName() {
      return "correlationInfo";
    }
  }

}
